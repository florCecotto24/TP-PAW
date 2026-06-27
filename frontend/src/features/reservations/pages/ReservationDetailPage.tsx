import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
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
import { formatDateTime, formatPrice, statusLabelKey } from '../format';
import { reservationActionError } from '../reservationError';
import {
  availableActions,
  cancelStatusFor,
  sideOf,
  type Side,
} from '../reservationActions';
import { useCurrentUser } from '../useCurrentUser';
import type { ReservationDto, ReviewDto } from '../types';

const REVIEW_COMMENT_MAX = 200;

function pickupAddress(reservation: ReservationDto): string {
  const parts = [
    reservation.pickupStreet,
    reservation.pickupNumber,
    reservation.pickupNeighborhood,
  ].filter(Boolean);
  return parts.join(' ') || '—';
}

function userAlreadyReviewed(reviews: ReviewDto[], side: Side): boolean {
  if (side === 'none') return true;
  const expectRider = side === 'rider';
  return reviews.some((r) => r.madeByRider === expectRider);
}

export default function ReservationDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const { id: myId, isAuthenticated } = useCurrentUser();

  const reservationUri = id ? `/reservations/${id}` : null;

  const reservationQuery = useQuery({
    queryKey: ['reservations', 'detail', id],
    queryFn: () => getReservation(reservationUri as string).then((r) => r.data),
    enabled: Boolean(reservationUri && isAuthenticated),
  });

  const reservation = reservationQuery.data;
  const side: Side = reservation && myId ? sideOf(reservation, myId) : 'none';
  const actions = reservation ? availableActions(reservation, side) : null;

  const carQuery = useQuery({
    queryKey: ['reservations', 'detail', 'car', reservation?.links.car],
    queryFn: () => getCar(reservation!.links.car).then((r) => r.data),
    enabled: Boolean(reservation?.links.car),
  });

  const counterpartyUri =
    reservation && side === 'rider'
      ? reservation.links.owner
      : reservation && side === 'owner'
        ? reservation.links.rider
        : null;

  const counterpartyQuery = useQuery({
    queryKey: ['reservations', 'detail', 'counterparty', counterpartyUri],
    queryFn: () => getCounterparty(counterpartyUri as string).then((r) => r.data),
    enabled: Boolean(counterpartyUri),
  });

  const reviewsUri = reservation?.links.reviews;
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

  const carId = reservation ? idFromUri(reservation.links.car) : null;
  const counterpartyId = counterpartyUri ? idFromUri(counterpartyUri) : null;

  const paymentReceiptLink = reservation?.links['payment-receipt'];
  const refundReceiptLink = reservation?.links['refund-receipt'];

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['reservations', 'detail', id] });
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
    } finally {
      setBusy(false);
    }
  };

  const onCancel = () => {
    if (!reservation || side === 'none') return;
    if (!window.confirm(t('res.detail.cancel'))) return;
    void runAction(async () => {
      await patchReservation(reservation.links.self, { status: cancelStatusFor(side) });
      setNotice(t('res.detail.cancelledNotice'));
    });
  };

  const onMarkReturned = () => {
    if (!reservation) return;
    if (!window.confirm(t('res.detail.markReturnedTitle'))) return;
    void runAction(async () => {
      await patchReservation(reservation.links.self, { carReturned: true });
      setNotice(t('res.detail.markReturnedNotice'));
    });
  };

  const onSaveDates = () => {
    if (!reservation || !editStart || !editEnd) return;
    const startIso = new Date(`${editStart}T00:00:00`).toISOString();
    const endIso = new Date(`${editEnd}T00:00:00`).toISOString();
    void runAction(async () => {
      await patchReservation(reservation.links.self, { startDate: startIso, endDate: endIso });
      setEditDatesOpen(false);
    });
  };

  const onUploadReceipt = (kind: 'payment' | 'refund', file: File) => {
    if (!reservation) return;
    const uri =
      kind === 'payment'
        ? reservation.links['payment-receipt']
        : reservation.links['refund-receipt'];
    if (!uri) return;
    void runAction(() => uploadReceipt(uri, file).then(() => undefined));
  };

  const onSubmitReview = () => {
    if (!reviewsUri) return;
    if (reviewComment.length > REVIEW_COMMENT_MAX) {
      setActionError(t('res.review.commentTooLong', { max: REVIEW_COMMENT_MAX }));
      return;
    }
    void runAction(async () => {
      await postReview(reviewsUri, {
        rating: reviewRating,
        comment: reviewComment,
        image: reviewImage,
      });
      setReviewComment('');
      setReviewImage(null);
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
        <Link to="/ingresar" className="btn btn-primary">{t('res.list.login')}</Link>
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
        <p className="text-secondary">{t('res.detail.loading')}</p>
      </main>
    );
  }

  if (reservationQuery.isError || !reservation) {
    return (
      <main className="container py-4">
        <div className="alert alert-danger">{t('res.detail.error')}</div>
        <Link to="/mis-reservas" className="btn btn-link">{t('res.list.title')}</Link>
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

  return (
    <main className="container py-4">
      <div className="mb-3">
        <Link
          to={side === 'owner' ? '/mis-autos/reservas' : '/mis-reservas'}
          className="btn btn-link ps-0"
        >
          ← {side === 'owner' ? t('res.list.ownerTitle') : t('res.list.title')}
        </Link>
      </div>

      <header className="mb-4">
        <h1 className="h3 fw-semibold mb-1">{t('res.detail.title')}</h1>
        <p className="text-secondary mb-0">{t('res.detail.subheading')}</p>
      </header>

      {notice ? <div className="alert alert-success">{notice}</div> : null}
      {actionError ? <div className="alert alert-danger">{actionError}</div> : null}

      <div className="row g-4">
        <div className="col-lg-8">
          <section className="bg-white rounded-4 shadow-sm p-4 mb-4">
            <h2 className="h5 fw-semibold mb-3">{t('res.detail.carSummaryTitle')}</h2>
            {carQuery.data ? (
              <>
                <p className="fw-semibold mb-1">
                  {carQuery.data.brandName} {carQuery.data.modelName}
                </p>
                {(transmissionLabel || powertrainLabel) ? (
                  <p className="small text-secondary mb-2">
                    {[transmissionLabel, powertrainLabel].filter(Boolean).join(' · ')}
                  </p>
                ) : null}
                {carId ? (
                  <Link to={`/autos/${carId}`} className="btn btn-outline-primary btn-sm">
                    {t('res.detail.viewCar')}
                  </Link>
                ) : null}
              </>
            ) : (
              <p className="text-secondary small mb-0">{t('res.detail.loading')}</p>
            )}
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

          {side === 'rider' && reservation.status === 'pending' ? (
            <section className="bg-white rounded-4 shadow-sm p-4 mb-4">
              <h2 className="h5 fw-semibold mb-2">{t('res.detail.payment.title')}</h2>
              <p className="small text-secondary">{t('res.detail.payment.intro')}</p>
              {reservation.ownerCbu ? (
                <p className="small mb-2">
                  <strong>{t('res.detail.payment.cbu')}</strong> {reservation.ownerCbu}
                </p>
              ) : null}
              {reservation.paymentProofDeadlineAt ? (
                <p className="small mb-0">
                  <strong>{t('res.detail.payment.deadline')}</strong>{' '}
                  {formatDateTime(reservation.paymentProofDeadlineAt)}
                </p>
              ) : null}
            </section>
          ) : null}

          <ReservationChatPanel reservation={reservation} />

          {reviews.length > 0 ? (
            <section className="bg-white rounded-4 shadow-sm p-4 mt-4">
              <h2 className="h5 fw-semibold mb-3">{t('res.review.existing')}</h2>
              <ul className="list-unstyled mb-0">
                {reviews.map((review) => (
                  <li key={review.links.self} className="border-bottom pb-3 mb-3">
                    <div className="small text-secondary mb-1">
                      {'★'.repeat(review.rating ?? 0)} · {formatDateTime(review.createdAt)}
                    </div>
                    {review.comment ? <p className="mb-2">{review.comment}</p> : null}
                    {review.links.image ? (
                      <button
                        type="button"
                        className="btn btn-link btn-sm p-0"
                        onClick={() => void openBinaryLink(review.links.image as string)}
                      >
                        {t('res.review.viewImage')}
                      </button>
                    ) : null}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}

          {showReviewForm ? (
            <section className="bg-white rounded-4 shadow-sm p-4 mt-4">
              <h2 className="h5 fw-semibold mb-3">{t('res.review.title')}</h2>
              <div className="mb-3">
                <label className="form-label small" htmlFor="reviewRating">{t('res.review.rating')}</label>
                <select
                  id="reviewRating"
                  className="form-select form-select-sm w-auto"
                  value={reviewRating}
                  onChange={(e) => setReviewRating(Number(e.target.value))}
                >
                  {[5, 4, 3, 2, 1].map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>
              <div className="mb-3">
                <label className="form-label small" htmlFor="reviewComment">{t('res.review.comment')}</label>
                <textarea
                  id="reviewComment"
                  className="form-control form-control-sm"
                  rows={3}
                  maxLength={REVIEW_COMMENT_MAX}
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                />
                <div className="form-text">
                  {t('res.review.commentCounter', { count: reviewComment.length, max: REVIEW_COMMENT_MAX })}
                </div>
              </div>
              <div className="mb-3">
                <label className="form-label small" htmlFor="reviewImage">{t('res.review.image')}</label>
                <input
                  id="reviewImage"
                  type="file"
                  className="form-control form-control-sm"
                  accept="image/*"
                  onChange={(e) => setReviewImage(e.target.files?.[0] ?? null)}
                />
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
          {counterpartyQuery.data ? (
            <aside className="bg-white rounded-4 shadow-sm p-4 mb-4">
              <h2 className="h6 fw-semibold mb-3">
                {side === 'rider'
                  ? t('res.detail.counterparty.ownerTitle')
                  : t('res.detail.counterparty.riderTitle')}
              </h2>
              <p className="mb-1 fw-semibold">
                {counterpartyQuery.data.forename} {counterpartyQuery.data.surname}
              </p>
              {counterpartyQuery.data.email ? (
                <p className="small mb-1">
                  <span className="text-secondary">{t('res.detail.counterparty.email')}: </span>
                  {counterpartyQuery.data.email}
                </p>
              ) : null}
              {counterpartyQuery.data.phoneNumber ? (
                <p className="small mb-2">
                  <span className="text-secondary">{t('res.detail.counterparty.phone')}: </span>
                  {counterpartyQuery.data.phoneNumber}
                </p>
              ) : null}
              {counterpartyId ? (
                <Link to={`/usuarios/${counterpartyId}`} className="btn btn-outline-secondary btn-sm">
                  {t('res.detail.counterparty.viewProfile')}
                </Link>
              ) : null}
            </aside>
          ) : null}

          <aside className="bg-white rounded-4 shadow-sm p-4">
            <h2 className="h6 fw-semibold mb-3">{t('res.detail.actionsTitle')}</h2>
            <div className="d-grid gap-2">
              {paymentReceiptLink ? (
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => void openBinaryLink(paymentReceiptLink)}
                >
                  {t('res.detail.downloadPayment')}
                </button>
              ) : null}

              {refundReceiptLink ? (
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => void openBinaryLink(refundReceiptLink)}
                >
                  {t('res.detail.downloadRefund')}
                </button>
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

              {actions?.canUploadPayment ? (
                <div>
                  <label className="form-label small mb-1" htmlFor="paymentFile">
                    {t('res.detail.uploadPayment')}
                  </label>
                  <input
                    id="paymentFile"
                    type="file"
                    className="form-control form-control-sm"
                    accept="image/*,application/pdf"
                    disabled={busy}
                    onChange={(e) => {
                      const f = e.target.files?.[0];
                      if (f) onUploadReceipt('payment', f);
                    }}
                  />
                </div>
              ) : null}

              {actions?.canUploadRefund ? (
                <div>
                  <label className="form-label small mb-1" htmlFor="refundFile">
                    {t('res.detail.uploadRefund')}
                  </label>
                  <input
                    id="refundFile"
                    type="file"
                    className="form-control form-control-sm"
                    accept="image/*,application/pdf"
                    disabled={busy}
                    onChange={(e) => {
                      const f = e.target.files?.[0];
                      if (f) onUploadReceipt('refund', f);
                    }}
                  />
                </div>
              ) : null}

              {actions?.canMarkReturned ? (
                <button
                  type="button"
                  className="btn btn-outline-primary btn-sm"
                  disabled={busy}
                  onClick={onMarkReturned}
                >
                  {t('res.detail.markReturned')}
                </button>
              ) : null}

              {actions?.canCancel ? (
                <button
                  type="button"
                  className="btn btn-outline-danger btn-sm"
                  disabled={busy}
                  onClick={onCancel}
                >
                  {t('res.detail.cancel')}
                </button>
              ) : null}
            </div>
          </aside>
        </div>
      </div>
    </main>
  );
}
