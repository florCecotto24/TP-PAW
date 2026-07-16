import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import BreadcrumbTrail from '../../../components/ryden/layout/BreadcrumbTrail';
import { LoadingBlock, ReceiptUploadPicker } from '../../../components/ryden';
import { getCar, getReservation, idFromUri, uploadReceipt } from '../api';
import { formatDateTime, formatPrice } from '../format';
import { paths, myReservationDetail } from '../../../routes/paths';
import { carDetailTo } from '../../../routes/navigationState';
import { useSessionStore } from '../../../session/sessionStore';

/** Espejo de reservationConfirmation.jsp: agradecimiento + subida de comprobante de pago. */
export default function ReservationConfirmationPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const currentUser = useSessionStore((s) => s.currentUser);

  const reservationUri = id ? `/reservations/${id}` : null;
  const reservationQuery = useQuery({
    queryKey: ['reservations', 'confirmation', id],
    queryFn: () => getReservation(reservationUri as string).then((r) => r.data),
    enabled: Boolean(reservationUri),
  });
  const reservation = reservationQuery.data;

  const carQuery = useQuery({
    queryKey: ['reservations', 'confirmation', 'car', reservation?.links.car],
    queryFn: () => getCar(reservation!.links.car).then((r) => r.data),
    enabled: Boolean(reservation?.links.car),
  });
  const car = carQuery.data;
  const carName = car ? `${car.brandName} ${car.modelName}` : '';

  const [uploading, setUploading] = useState(false);
  const [uploadDone, setUploadDone] = useState(false);
  const [lastOwnerCbu, setLastOwnerCbu] = useState<string | null>(null);

  useEffect(() => {
    setLastOwnerCbu(null);
  }, [id]);

  useEffect(() => {
    if (reservation?.ownerCbu) {
      setLastOwnerCbu(reservation.ownerCbu);
    }
  }, [reservation?.ownerCbu]);

  if (!id) {
    return (
      <main className="container py-5">
        <div className="alert alert-warning">{t('res.new.missingParams')}</div>
      </main>
    );
  }

  const onConfirmUpload = async (file: File) => {
    if (!reservation) throw new Error('missing reservation');
    const uri = reservation.links['payment-receipt'];
    if (!uri) throw new Error('missing payment-receipt link');
    setUploading(true);
    try {
      await uploadReceipt(uri, file);
      setUploadDone(true);
      await queryClient.invalidateQueries({ queryKey: ['reservations', 'confirmation', id] });
    } finally {
      setUploading(false);
    }
  };

  const heading = t('res.confirmation.title');
  const receiptLocked = Boolean(reservation?.hasPaymentReceipt || uploadDone);
  const ownerCbuLabel = reservation?.ownerCbu ?? lastOwnerCbu ?? '—';
  const carDetailLink = reservation?.links?.car
    ? carDetailTo(idFromUri(reservation.links.car) ?? '', reservation.links.car)
    : null;

  return (
    <main className="container py-5 reservation-confirmation">
      <BreadcrumbTrail
        homeLabel={t('nav.myReservations')}
        homeHref={paths.myReservations}
        currentLabel={heading}
      />
      <div className="row justify-content-center">
        <div className="col-md-9 col-lg-7">
          <div className="card border-0 shadow-sm rounded-4 bg-white">
            <div className="card-body p-4 p-md-5">
              {reservationQuery.isLoading ? (
                <LoadingBlock variant="page" className="py-3" />
              ) : null}
              {reservationQuery.isError || !reservation ? (
                <div className="alert alert-danger">{t('res.detail.error')}</div>
              ) : (
                <>
                  <div className="text-center mb-4">
                    <h1 className="h3 fw-bold mb-3">{t('res.confirmation.heading')}</h1>
                    <p className="mb-2">
                      {t('res.confirmation.greetingBefore')}{' '}
                      {currentUser?.forename} {currentUser?.surname}
                      {t('res.confirmation.greetingAfter')}
                    </p>
                    <p className="text-secondary mb-3">
                      {t('res.confirmation.contactBeforeCar')} <strong>{carName}</strong>{' '}
                      {t('res.confirmation.contactAfterCar')} <strong>{currentUser?.email}</strong>{' '}
                      {t('res.confirmation.contactAfterEmail')}
                    </p>
                    {reservation.paymentProofDeadlineAt ? (
                      <div className="small text-secondary mx-auto" style={{ maxWidth: '32rem' }}>
                        <p className="mb-0">
                          {t('res.confirmation.paymentProofNoticeUntil', {
                            deadline: formatDateTime(reservation.paymentProofDeadlineAt),
                          })}
                        </p>
                      </div>
                    ) : null}
                  </div>

                  <section className="border border-warning rounded-3 p-3 p-md-4 bg-warning-subtle mx-auto mb-4">
                    <article className="card border-0 shadow-sm rounded-4 mb-4">
                      <div className="card-body p-4">
                        <h2 className="h5 fw-semibold mb-3">{t('res.confirmation.paymentInfoTitle')}</h2>
                        <div className="row g-3">
                          <div className="col-sm-6">
                            <p className="mb-1 small text-secondary">{t('res.confirmation.totalPrice')}</p>
                            <p className="mb-0 fw-medium text-primary h5">
                              {formatPrice(reservation.totalPrice)}
                            </p>
                          </div>
                          <div className="col-sm-6">
                            <p className="mb-1 small text-secondary">{t('res.confirmation.ownerCbu')}</p>
                            <p className="mb-0 fw-medium">{ownerCbuLabel}</p>
                          </div>
                        </div>
                      </div>
                    </article>

                    {receiptLocked ? (
                      <p className="text-success small mb-0 text-center">
                        <i className="bi bi-check-circle-fill me-1" aria-hidden="true"></i>
                        {t('res.confirmation.uploadSuccess')}
                      </p>
                    ) : (
                      <ReceiptUploadPicker
                        busy={uploading}
                        onConfirm={onConfirmUpload}
                      />
                    )}
                  </section>

                  <div className="d-flex flex-wrap justify-content-center align-items-center gap-2">
                    <Link
                      to={myReservationDetail(id, { role: 'rider' })}
                      className="btn btn-sm btn-outline-primary px-3"
                    >
                      {t('res.confirmation.viewReservation')}
                    </Link>
                    {carDetailLink ? (
                      <Link
                        to={carDetailLink.pathname}
                        state={carDetailLink.state}
                        className="btn btn-sm btn-outline-secondary px-3"
                      >
                        {t('res.confirmation.viewCar')}
                      </Link>
                    ) : null}
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
