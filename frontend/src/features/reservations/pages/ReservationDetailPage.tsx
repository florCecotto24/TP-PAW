import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useParams, useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { BreadcrumbTrail, ConfirmModal, LoadingBlock, ReceiptUploadPicker, ReviewImageInput, StarRatingInput, Avatar } from '../../../components/ryden';
import { carCoverAssetUrl, profilePictureAssetUrl } from '../../../api/uri';
import {
  getCar,
  getCounterparty,
  getReservation,
  idFromUri,
  listReviews,
  openBinaryLink,
  patchReservation,
  postReview,
  uploadReceipt,
} from '../api';
import ReservationChatPanel from '../components/ReservationChatPanel';
import ReservationReviewItem from '../components/ReservationReviewItem';
import { formatDateTime, formatPrice, statusLabelKey } from '../format';
import { isChatAvailable } from '../reservationChat';
import { paths, ownerReservationsCar } from '../../../routes/paths';
import { carDetailTo, publicProfileTo, type ReservationDetailLocationState } from '../../../routes/navigationState';
import { resolveResourceUri } from '../../../api/resourceUri';
import { reservationActionError } from '../reservationError';
import {
  availableActions,
  cancelStatusFor,
  sideOf,
  type Side,
} from '../reservationActions';
import { useCurrentUser } from '../useCurrentUser';
import type { ReservationDto, ReviewDto } from '../types';

import { getClientConfig } from '../../../api/clientConfig';

function pickupAddress(reservation: ReservationDto): string {
  const parts = [
    reservation.pickupStreet,
    reservation.pickupNumber,
    reservation.pickupNeighborhood,
  ].filter(Boolean);
  return parts.join(' ') || '—';
}

/** Wall-clock handover as {@code HH:mm:ss} for PATCH date payloads (keeps existing times). */
function normalizeHandoverTime(hhmm: string | null | undefined): string {
  if (!hhmm || !hhmm.trim()) return '00:00:00';
  const t = hhmm.trim();
  if (/^\d{2}:\d{2}:\d{2}$/.test(t)) return t;
  if (/^\d{2}:\d{2}$/.test(t)) return `${t}:00`;
  return '00:00:00';
}

function userAlreadyReviewed(reviews: ReviewDto[], side: Side): boolean {
  if (side === 'none') return true;
  const expectRider = side === 'rider';
  return reviews.some((r) => r.madeByRider === expectRider);
}

export default function ReservationDetailPage() {
  const { t } = useTranslation();
  const reviewCommentMaxLength = getClientConfig().review.commentMaxLength;
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const reservationSelfFromNav = (location.state as ReservationDetailLocationState | null)?.reservationSelf;
  const [searchParams] = useSearchParams();
  const fromCarId = searchParams.get('fromCar');
  const queryClient = useQueryClient();
  const { id: myId, isAuthenticated } = useCurrentUser();

  const reservationUri = resolveResourceUri({
    stateUri: reservationSelfFromNav,
    routeId: id,
    collection: 'reservations',
  });

  const reservationQuery = useQuery({
    queryKey: ['reservations', 'detail', reservationUri],
    queryFn: () => getReservation(reservationUri as string).then((r) => r.data),
    enabled: Boolean(reservationUri && isAuthenticated),
  });

  const reservation = reservationQuery.data;
  const side: Side = reservation && myId ? sideOf(reservation, myId) : 'none';
  const actions = reservation ? availableActions(reservation, side) : null;
  const chatAvailable = reservation ? isChatAvailable(reservation) : false;

  const reservationInfoAlert = useMemo(() => {
    if (!reservation) return null;
    const status = reservation.status;
    if (status === 'pending' && side === 'owner') return t('res.detail.alert.pendingPaymentOwner');
    if (status.startsWith('cancelled')) return t('res.detail.alert.cancelled');
    if (status === 'started') return t('res.detail.alert.inProgress');
    if (status === 'finished') return t('res.detail.alert.finished');
    return null;
  }, [reservation, side, t]);

  const carQuery = useQuery({
    queryKey: ['reservations', 'detail', 'car', reservation?.links?.car],
    queryFn: () => getCar(reservation!.links.car).then((r) => r.data),
    enabled: Boolean(reservation?.links?.car),
  });

  const counterpartyUri = reservation?.links?.counterparty ?? null;

  const counterpartyQuery = useQuery({
    queryKey: ['reservations', 'detail', 'counterparty', counterpartyUri],
    queryFn: () => getCounterparty(counterpartyUri as string).then((r) => r.data),
    enabled: Boolean(counterpartyUri),
  });

  const reviewsUri = reservation?.links?.reviews;
  const reviewsQuery = useQuery({
    queryKey: ['reservations', 'detail', 'reviews', reviewsUri],
    queryFn: () => listReviews(reviewsUri as string).then((r) => r.data ?? []),
    enabled: Boolean(reviewsUri),
  });

  const reviews = reviewsQuery.data ?? [];
  const showReviewForm = Boolean(actions?.canReview && !userAlreadyReviewed(reviews, side));

  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const [editDatesOpen, setEditDatesOpen] = useState(false);
  const [editStart, setEditStart] = useState('');
  const [editEnd, setEditEnd] = useState('');

  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewImage, setReviewImage] = useState<File | null>(null);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [returnModalOpen, setReturnModalOpen] = useState(false);
  const [chatExpanded, setChatExpanded] = useState(false);
  const [carImageFailed, setCarImageFailed] = useState(false);

  const carId = reservation ? idFromUri(reservation.links?.car) : null;
  const carSelf = reservation?.links?.car ?? null;
  const carDetailLink = carId ? carDetailTo(carId, carSelf) : null;
  const carCoverUrl = useMemo(
    () => carCoverAssetUrl(reservation?.links?.car, carQuery.data?.links?.cover),
    [reservation?.links?.car, carQuery.data?.links?.cover],
  );

  useEffect(() => {
    setCarImageFailed(false);
  }, [carCoverUrl]);
  const counterpartyAvatarUrl = profilePictureAssetUrl(counterpartyQuery.data?.links);
  const counterpartyId = counterpartyQuery.data?.links?.self
    ? idFromUri(counterpartyQuery.data.links.self)
    : null;
  const counterpartyProfileLink = counterpartyId
    ? publicProfileTo(counterpartyId, counterpartyQuery.data?.links?.self)
    : null;

  const paymentReceiptDownloadLink =
    reservation?.hasPaymentReceipt && reservation.links['payment-receipt']
      ? reservation.links['payment-receipt']
      : null;
  const refundReceiptDownloadLink =
    reservation?.hasRefundReceipt && reservation.links['refund-receipt']
      ? reservation.links['refund-receipt']
      : null;

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['reservations', 'detail', reservationUri] });
    if (reviewsUri) {
      await queryClient.invalidateQueries({ queryKey: ['reservations', 'detail', 'reviews', reviewsUri] });
    }
  };

  const runAction = async (fn: () => Promise<void>, successMsg?: string) => {
    setBusy(true);
    setActionError(null);
    try {
      await fn();
      await invalidate();
      if (successMsg) setNotice(successMsg);
    } catch (err) {
      setActionError(reservationActionError(t, err, 'detail'));
      throw err;
    } finally {
      setBusy(false);
    }
  };

  const onCancel = () => {
    if (!reservation || side === 'none') return;
    void runAction(async () => {
      await patchReservation(reservation.links.self, { status: cancelStatusFor(side) });
      setNotice(t('res.detail.cancelledNotice'));
    });
    setCancelModalOpen(false);
  };

  const onMarkReturned = () => {
    if (!reservation) return;
    void runAction(async () => {
      await patchReservation(reservation.links.self, { carReturned: true });
      setNotice(t('res.detail.markReturnedNotice'));
    });
    setReturnModalOpen(false);
  };

  const onSaveDates = () => {
    if (!reservation || !editStart || !editEnd) return;
    // Preserve existing handover wall-clock times (P3-6); date-only edit must not force midnight.
    const checkIn = normalizeHandoverTime(reservation.checkInTime);
    const checkOut = normalizeHandoverTime(reservation.checkOutTime);
    const startIso = `${editStart}T${checkIn}`;
    const endIso = `${editEnd}T${checkOut}`;
    void runAction(async () => {
      await patchReservation(reservation.links.self, { startDate: startIso, endDate: endIso });
      setEditDatesOpen(false);
      setNotice(t('res.detail.datesSavedNotice'));
    });
  };

  const onUploadReceipt = async (kind: 'payment' | 'refund', file: File) => {
    if (!reservation) throw new Error('missing reservation');
    const uri =
      kind === 'payment'
        ? reservation.links['payment-receipt']
        : reservation.links['refund-receipt'];
    if (!uri) throw new Error(`missing ${kind}-receipt link`);
    await runAction(
      () => uploadReceipt(uri, file).then(() => undefined),
      t('res.confirmation.uploadSuccess'),
    );
  };

  const onSubmitReview = () => {
    if (!reviewsUri || !reservation?.links?.self) return;
    if (reviewComment.length > reviewCommentMaxLength) {
      setActionError(t('res.review.commentTooLong', { max: reviewCommentMaxLength }));
      return;
    }
    void runAction(async () => {
      await postReview(reviewsUri, reservation.links.self, {
        rating: reviewRating,
        comment: reviewComment,
        image: reviewImage,
      });
      setReviewComment('');
      setReviewImage(null);
      setNotice(t('res.review.success'));
    });
  };

  const transmissionLabel = useMemo(() => {
    const key = carQuery.data?.transmission;
    return key ? t(`res.car.transmission.${key}` as const) : null;
  }, [carQuery.data?.transmission, t]);

  const powertrainLabel = useMemo(() => {
    const key = carQuery.data?.powertrain;
    return key ? t(`res.car.powertrain.${key}` as const) : null;
  }, [carQuery.data?.powertrain, t]);

  if (!isAuthenticated) {
    return (
      <main className="container py-4">
        <div className="alert alert-warning">{t('res.list.needLogin')}</div>
        <Link to={paths.login} className="btn btn-primary">{t('res.list.login')}</Link>
      </main>
    );
  }

  if (!id) {
    return (
      <main className="container py-4">
        <div className="alert alert-warning">{t('res.detail.notFound')}</div>
      </main>
    );
  }

  if (reservationQuery.isLoading) {
    return (
      <main className="container py-4">
        <LoadingBlock variant="page" className="py-4" />
      </main>
    );
  }

  if (reservationQuery.isError || !reservation) {
    return (
      <main className="container py-4">
        <div className="alert alert-danger">{t('res.detail.error')}</div>
        <Link to={paths.myReservations} className="btn btn-link">{t('res.list.title')}</Link>
      </main>
    );
  }

  if (side === 'none') {
    return (
      <main className="container py-4">
        <div className="alert alert-danger">{t('res.detail.actionError.forbidden')}</div>
      </main>
    );
  }

  const carLabel = carQuery.data ? `${carQuery.data.brandName} ${carQuery.data.modelName}` : '';

  return (
    <main className="container pt-5 pb-4">
      {fromCarId ? (
        <BreadcrumbTrail
          homeLabel={t('res.list.ownerTitle')}
          homeHref={paths.ownerReservations}
          midLabel={carLabel}
          midHref={ownerReservationsCar(fromCarId)}
          currentLabel={t('res.detail.title')}
        />
      ) : (
        <BreadcrumbTrail
          homeLabel={side === 'owner' ? t('res.list.ownerTitle') : t('res.list.title')}
          homeHref={side === 'owner' ? paths.ownerReservations : paths.myReservations}
          currentLabel={carLabel || t('res.detail.title')}
        />
      )}

      <section className="reservation-management-header mb-4">
        <h1 className="h3 fw-bold mb-2">{t('res.detail.title')}</h1>
        <p className="text-secondary mb-0">{t('res.detail.subheading')}</p>
      </section>

      {notice ? <div className="alert alert-success">{notice}</div> : null}
      {actionError ? <div className="alert alert-danger">{actionError}</div> : null}

      <div className="row g-4 align-items-start">
        <div className="col-lg-8">
          {side === 'rider' && actions?.canUploadPayment ? (
            <section className="card border border-warning shadow-sm rounded-4 mb-4 bg-warning-subtle">
              <div className="card-body p-4">
                <h2 className="h5 fw-semibold mb-3">
                  <i className="bi bi-clock text-primary me-1" aria-hidden="true"></i>
                  {t('res.detail.payment.title')}
                </h2>
                <p className="text-secondary mb-3">{t('res.detail.payment.intro')}</p>
                {reservation.ownerCbu ? (
                  <p className="mb-3">
                    <span className="fw-semibold">{t('res.detail.payment.cbu')}</span> {reservation.ownerCbu}
                  </p>
                ) : null}
                {reservation.paymentProofDeadlineAt ? (
                  <p className="mb-3">
                    <span className="fw-semibold">{t('res.detail.payment.deadline')}</span>{' '}
                    {formatDateTime(reservation.paymentProofDeadlineAt)}
                  </p>
                ) : null}
                <label className="form-label small mb-1" htmlFor="paymentFile">
                  {t('res.detail.uploadPayment')}
                </label>
                <ReceiptUploadPicker
                  id="paymentFile"
                  busy={busy}
                  onConfirm={(file) => onUploadReceipt('payment', file)}
                />
              </div>
            </section>
          ) : null}

          {side === 'rider' && reservation.hasPaymentReceipt ? (
            <section className="card border-0 shadow-sm rounded-4 mb-4 bg-white">
              <div className="card-body p-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
                <div>
                  <h2 className="h6 fw-semibold mb-1">{t('res.detail.payment.viewReceipt')}</h2>
                </div>
                {paymentReceiptDownloadLink ? (
                  <button
                    type="button"
                    className="btn btn-outline-primary"
                    onClick={() => void openBinaryLink(paymentReceiptDownloadLink)}
                  >
                    {t('res.detail.payment.viewReceipt')}
                  </button>
                ) : null}
              </div>
            </section>
          ) : null}

          {side === 'owner'
          && reservation.hasPaymentReceipt
          && ['accepted', 'started', 'finished'].includes(reservation.status) ? (
            <section className="card border-0 shadow-sm rounded-4 mb-4 bg-white">
              <div className="card-body p-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
                <h2 className="h6 fw-semibold mb-0">{t('res.detail.payment.ownerTitle')}</h2>
                {paymentReceiptDownloadLink ? (
                  <button
                    type="button"
                    className="btn btn-outline-primary"
                    onClick={() => void openBinaryLink(paymentReceiptDownloadLink)}
                  >
                    {t('res.detail.payment.viewReceipt')}
                  </button>
                ) : null}
              </div>
            </section>
          ) : null}

          {actions?.canUploadRefund ? (
            <section className="card border-0 shadow-sm rounded-4 mb-4 border-warning bg-white">
              <div className="card-body p-4">
                <h2 className="h5 fw-semibold mb-3">{t('res.detail.refund.ownerTitle')}</h2>
                <p className="text-secondary mb-3">{t('res.detail.refund.ownerIntro')}</p>
                {reservation.refundProofDeadlineAt ? (
                  <p className="mb-3">
                    <span className="fw-semibold">{t('res.detail.refund.deadline')}</span>{' '}
                    {formatDateTime(reservation.refundProofDeadlineAt)}
                  </p>
                ) : null}
                <label className="form-label small mb-1" htmlFor="refundFile">
                  {t('res.detail.uploadRefund')}
                </label>
                <ReceiptUploadPicker
                  id="refundFile"
                  busy={busy}
                  onConfirm={(file) => onUploadReceipt('refund', file)}
                />
              </div>
            </section>
          ) : null}

          {side === 'rider'
          && reservation.paymentRefundRequired
          && !reservation.hasRefundReceipt
          && (reservation.status === 'cancelled_by_owner' || reservation.status === 'cancelled_by_rider') ? (
            <section className="card border-0 shadow-sm rounded-4 mb-4 bg-white">
              <div className="card-body p-4">
                <h2 className="h5 fw-semibold mb-2">{t('res.detail.refund.riderWaitingTitle')}</h2>
                <p className="text-secondary small mb-0">{t('res.detail.refund.riderWaitingIntro')}</p>
              </div>
            </section>
          ) : null}

          {reservation.hasRefundReceipt ? (
            <section className="card border-0 shadow-sm rounded-4 mb-4 bg-white">
              <div className="card-body p-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
                <h2 className="h6 fw-semibold mb-0">
                  {side === 'owner' ? t('res.detail.refund.ownerUploadedTitle') : t('res.detail.refund.riderTitle')}
                </h2>
                {refundReceiptDownloadLink ? (
                  <button
                    type="button"
                    className="btn btn-outline-primary"
                    onClick={() => void openBinaryLink(refundReceiptDownloadLink)}
                  >
                    {t('res.detail.refund.viewReceipt')}
                  </button>
                ) : null}
              </div>
            </section>
          ) : null}

          <section className="bg-white rounded-4 shadow-sm p-4 mb-4">
            <h2 className="h5 fw-semibold mb-3">{t('res.detail.carSummaryTitle')}</h2>
            <div className="d-flex flex-column flex-md-row gap-3 align-items-start">
              <div className="reservation-detail-car-media rounded-3 overflow-hidden border">
                {carCoverUrl && !carImageFailed ? (
                  <img
                    src={carCoverUrl}
                    alt={carLabel || t('res.detail.carSummaryTitle')}
                    className="w-100 h-100"
                    style={{ objectFit: 'cover' }}
                    onError={() => setCarImageFailed(true)}
                  />
                ) : (
                  <div className="w-100 h-100 d-flex align-items-center justify-content-center text-secondary bg-body-tertiary">
                    <i className="bi bi-car-front fs-1" aria-hidden="true"></i>
                  </div>
                )}
              </div>
              <div className="min-w-0">
                {carQuery.isLoading ? (
                  <LoadingBlock variant="inline" />
                ) : carQuery.isError || !carQuery.data ? (
                  <p className="text-secondary mb-0">{t('res.detail.carLoadError')}</p>
                ) : (
                  <>
                    <p className="fw-semibold mb-1">
                      {carQuery.data.brandName} {carQuery.data.modelName}
                    </p>
                    {(transmissionLabel || powertrainLabel) ? (
                      <div className="d-flex flex-wrap gap-2 mb-2">
                        {transmissionLabel ? <span className="badge text-bg-light border">{transmissionLabel}</span> : null}
                        {powertrainLabel ? <span className="badge text-bg-light border">{powertrainLabel}</span> : null}
                      </div>
                    ) : null}
                    {carDetailLink ? (
                      <Link
                        to={carDetailLink.pathname}
                        state={carDetailLink.state}
                        className="btn btn-outline-primary btn-sm"
                      >
                        {t('res.detail.viewCar')}
                      </Link>
                    ) : null}
                  </>
                )}
              </div>
            </div>
          </section>

          <section className="bg-white rounded-4 shadow-sm p-4 mb-4">
            <h2 className="h5 fw-semibold mb-3">{t('res.detail.detailsTitle')}</h2>
            <dl className="row small mb-0">
              <dt className="col-sm-4 text-secondary">{t('res.detail.status')}</dt>
              <dd className="col-sm-8">
                <span className="badge text-bg-secondary">{t(statusLabelKey(reservation.status))}</span>
              </dd>

              <dt className="col-sm-4 text-secondary">{t('res.detail.pickup')}</dt>
              <dd className="col-sm-8">{formatDateTime(reservation.startDate)}</dd>

              <dt className="col-sm-4 text-secondary">{t('res.detail.return')}</dt>
              <dd className="col-sm-8">{formatDateTime(reservation.endDate)}</dd>

              <dt className="col-sm-4 text-secondary">{t('res.detail.total')}</dt>
              <dd className="col-sm-8 fw-semibold">{formatPrice(reservation.totalPrice)}</dd>

              {(reservation.pickupStreet || reservation.pickupNeighborhood) ? (
                <>
                  <dt className="col-sm-4 text-secondary">{t('res.detail.pickupLocation')}</dt>
                  <dd className="col-sm-8">{pickupAddress(reservation)}</dd>
                </>
              ) : null}

              {(reservation.checkInTime || reservation.checkOutTime) ? (
                <>
                  <dt className="col-sm-4 text-secondary">{t('res.detail.handoverTimes')}</dt>
                  <dd className="col-sm-8">
                    {t('res.detail.handoverTimesValue', {
                      checkIn: reservation.checkInTime ?? '—',
                      checkOut: reservation.checkOutTime ?? '—',
                    })}
                  </dd>
                </>
              ) : null}

              {reservation.paymentProofDeadlineAt ? (
                <>
                  <dt className="col-sm-4 text-secondary">{t('res.detail.paymentDeadline')}</dt>
                  <dd className="col-sm-8">{formatDateTime(reservation.paymentProofDeadlineAt)}</dd>
                </>
              ) : null}

              {reservation.refundProofDeadlineAt ? (
                <>
                  <dt className="col-sm-4 text-secondary">{t('res.detail.refundDeadline')}</dt>
                  <dd className="col-sm-8">{formatDateTime(reservation.refundProofDeadlineAt)}</dd>
                </>
              ) : null}
            </dl>
          </section>

          {reviews.length > 0 ? (
            <section className="bg-white rounded-4 shadow-sm p-4 mt-4">
              <h2 className="h5 fw-semibold mb-3">{t('res.review.existing')}</h2>
              <div className="d-flex flex-column gap-3">
                {reviews.map((review) => (
                  <ReservationReviewItem key={review.links.self} review={review} />
                ))}
              </div>
            </section>
          ) : null}

          {showReviewForm ? (
            <section className="bg-white rounded-4 shadow-sm p-4 mt-4">
              <h2 className="h5 fw-semibold mb-2">
                {t(side === 'owner' ? 'res.review.titleOwner' : 'res.review.titleRider')}
              </h2>
              <p className="text-secondary small mb-2">
                {t(side === 'owner' ? 'res.review.introOwner' : 'res.review.introRider')}
              </p>
              <p className="text-secondary small mb-3">
                {t(side === 'owner' ? 'res.review.autoSkipNoticeOwner' : 'res.review.autoSkipNoticeRider')}
              </p>
              <div className="mb-3">
                <label className="form-label small">{t('res.review.rating')}</label>
                <StarRatingInput id="reviewRating" value={reviewRating} onChange={setReviewRating} />
              </div>
              <div className="mb-3">
                <label className="form-label small" htmlFor="reviewComment">{t('res.review.comment')}</label>
                <textarea
                  id="reviewComment"
                  className="form-control form-control-sm"
                  rows={3}
                  maxLength={reviewCommentMaxLength}
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                />
                <div className="form-text">
                  {t('res.review.commentCounter', { count: reviewComment.length, max: reviewCommentMaxLength })}
                </div>
              </div>
              <div className="mb-3">
                <span className="form-label small d-block">{t('res.review.image')}</span>
                <ReviewImageInput id="reviewImage" value={reviewImage} onChange={setReviewImage} />
              </div>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                disabled={busy}
                onClick={() => onSubmitReview()}
              >
                {busy ? t('res.review.submitting') : t('res.review.submit')}
              </button>
            </section>
          ) : null}
        </div>

        <div className="col-lg-4">
          <aside className="card border-0 shadow-sm rounded-4 reservation-detail-sticky bg-white">
            <div className="card-body p-4">
              <div className="reservation-price-compact mb-3">
                <span className="reservation-card__meta-label mb-0">{t('res.detail.total')}</span>
                <span className="h2 fw-bold text-primary mb-0">{formatPrice(reservation.totalPrice)}</span>
              </div>

              {chatAvailable ? (
                <button
                  type="button"
                  className="btn btn-primary w-100 mb-3"
                  onClick={() => setChatExpanded(true)}
                >
                  {t('res.detail.openChat')}
                </button>
              ) : null}

              {counterpartyQuery.data ? (
                <section className="card border-0 shadow-sm rounded-4 mb-3 counterparty-summary-card">
                  <div className="card-body p-4">
                    <h2 className="h6 fw-semibold mb-3">
                      {side === 'rider'
                        ? t('res.detail.counterparty.ownerTitle')
                        : t('res.detail.counterparty.riderTitle')}
                    </h2>
                    <div className="counterparty-summary-card__identity d-flex align-items-center gap-2 mb-3">
                      {counterpartyProfileLink ? (
                        <Link
                          to={counterpartyProfileLink.pathname}
                          state={counterpartyProfileLink.state}
                          className="text-decoration-none"
                        >
                          <Avatar
                            src={counterpartyAvatarUrl}
                            forename={counterpartyQuery.data.forename}
                            surname={counterpartyQuery.data.surname}
                            className="rounded-circle border flex-shrink-0 counterparty-summary-card__avatar"
                            imgClassName="rounded-circle border flex-shrink-0 counterparty-summary-card__avatar"
                            placeholderClassName="rounded-circle border flex-shrink-0 counterparty-summary-card__avatar counterparty-summary-card__avatar--placeholder d-flex align-items-center justify-content-center"
                            barePhoto
                            iconFallback
                          />
                        </Link>
                      ) : null}
                      <p className="mb-0 fw-semibold">
                        {counterpartyQuery.data.forename} {counterpartyQuery.data.surname}
                      </p>
                    </div>
                    {counterpartyQuery.data.email ? (
                      <p className="small mb-1 counterparty-summary-card__email-wrap">
                        <span className="text-secondary">{t('res.detail.counterparty.email')}: </span>
                        <a href={`mailto:${counterpartyQuery.data.email}`} className="counterparty-summary-card__email-link">
                          {counterpartyQuery.data.email}
                        </a>
                      </p>
                    ) : null}
                    {counterpartyQuery.data.phoneNumber ? (
                      <p className="small mb-2">
                        <span className="text-secondary">{t('res.detail.counterparty.phone')}: </span>
                        <a href={`tel:${counterpartyQuery.data.phoneNumber}`}>{counterpartyQuery.data.phoneNumber}</a>
                      </p>
                    ) : null}
                    {counterpartyProfileLink ? (
                      <Link
                        to={counterpartyProfileLink.pathname}
                        state={counterpartyProfileLink.state}
                        className="btn btn-outline-secondary btn-sm"
                      >
                        {t('res.detail.counterparty.viewProfile')}
                      </Link>
                    ) : null}
                  </div>
                </section>
              ) : null}

              <div className="d-flex flex-column gap-2">
              {carDetailLink ? (
                <Link
                  to={carDetailLink.pathname}
                  state={carDetailLink.state}
                  className="btn btn-outline-warm w-100"
                >
                  <i className="bi bi-eye me-2"></i>{t('res.detail.viewCar')}
                </Link>
              ) : null}

              {actions?.canEditDates ? (
                <>
                  {!editDatesOpen ? (
                    <button
                      type="button"
                      className="btn btn-outline-primary btn-sm"
                      disabled={busy}
                      onClick={() => {
                        setEditStart(reservation.startDate.slice(0, 10));
                        setEditEnd(reservation.endDate.slice(0, 10));
                        setEditDatesOpen(true);
                      }}
                    >
                      {t('res.detail.editDates')}
                    </button>
                  ) : (
                    <div className="border rounded-3 p-3 bg-light">
                      <p className="small fw-semibold mb-2">{t('res.detail.editDatesTitle')}</p>
                      <label className="form-label small" htmlFor="editStart">{t('res.detail.newStart')}</label>
                      <input
                        id="editStart"
                        type="date"
                        className="form-control form-control-sm mb-2"
                        value={editStart}
                        onChange={(e) => setEditStart(e.target.value)}
                      />
                      <label className="form-label small" htmlFor="editEnd">{t('res.detail.newEnd')}</label>
                      <input
                        id="editEnd"
                        type="date"
                        className="form-control form-control-sm mb-2"
                        value={editEnd}
                        onChange={(e) => setEditEnd(e.target.value)}
                      />
                      <div className="d-flex gap-2">
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          disabled={busy}
                          onClick={onSaveDates}
                        >
                          {t('res.detail.saveDates')}
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline-secondary btn-sm"
                          onClick={() => setEditDatesOpen(false)}
                        >
                          {t('res.detail.cancelEdit')}
                        </button>
                      </div>
                    </div>
                  )}
                </>
              ) : null}

              {actions?.canMarkReturned ? (
                <button
                  type="button"
                  className="btn btn-primary w-100"
                  disabled={busy}
                  onClick={() => setReturnModalOpen(true)}
                >
                  {t('res.detail.markReturned')}
                </button>
              ) : null}

              {actions?.canCancel ? (
                <button
                  type="button"
                  className="btn btn-outline-danger w-100"
                  disabled={busy}
                  onClick={() => setCancelModalOpen(true)}
                >
                  <i className="bi bi-x-circle me-2"></i>{t('res.detail.cancelAction')}
                </button>
              ) : reservationInfoAlert ? (
                <div className="alert alert-info mb-0" role="alert">
                  <p className="mb-0">{reservationInfoAlert}</p>
                </div>
              ) : null}
              </div>
            </div>
          </aside>
        </div>
      </div>

      <ConfirmModal
        id="reservationCancelModal"
        title={t('res.detail.cancelModalTitle')}
        message={t('res.detail.cancelModalMessage')}
        action="#"
        cancelLabel={t('res.detail.cancelEdit')}
        confirmLabel={t('res.detail.cancelAction')}
        variant="danger"
        confirmButtonClass="btn btn-danger"
        open={cancelModalOpen}
        onOpenChange={setCancelModalOpen}
        onSubmit={(e) => {
          e.preventDefault();
          onCancel();
        }}
      />

      <ConfirmModal
        id="reservationReturnModal"
        title={t('res.detail.markReturnedTitle')}
        message={t('res.detail.markReturnedConfirm')}
        action="#"
        cancelLabel={t('res.detail.cancelEdit')}
        confirmLabel={t('res.detail.markReturned')}
        open={returnModalOpen}
        onOpenChange={setReturnModalOpen}
        onSubmit={(e) => {
          e.preventDefault();
          onMarkReturned();
        }}
      />

      {chatAvailable && reservation ? (
        <ReservationChatPanel
          reservation={reservation}
          expanded={chatExpanded}
          onExpandedChange={setChatExpanded}
          counterparty={
            counterpartyQuery.data
              ? {
                  name: `${counterpartyQuery.data.forename} ${counterpartyQuery.data.surname}`.trim(),
                  forename: counterpartyQuery.data.forename,
                  surname: counterpartyQuery.data.surname,
                  avatarUrl: counterpartyAvatarUrl,
                }
              : undefined
          }
        />
      ) : null}
    </main>
  );
}
