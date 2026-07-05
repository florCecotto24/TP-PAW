import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import BreadcrumbTrail from '../../../components/ryden/layout/BreadcrumbTrail';
import { getCar, getReservation, idFromUri, uploadReceipt } from '../api';
import { formatDateTime, formatPrice } from '../format';
import { paths, carDetail, myReservationDetail } from '../../../routes/paths';
import { useSessionStore } from '../../../session/sessionStore';

const MAX_RECEIPT_BYTES = 8 * 1024 * 1024;

/** Espejo de reservationConfirmation.jsp: agradecimiento + subida de comprobante de pago. */
export default function ReservationConfirmationPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const currentUser = useSessionStore((s) => s.currentUser);
  const fileInputRef = useRef<HTMLInputElement>(null);

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

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadDone, setUploadDone] = useState(false);

  if (!id) {
    return (
      <main className="container py-5">
        <div className="alert alert-warning">{t('res.new.missingParams')}</div>
      </main>
    );
  }

  const validateFile = (file: File): string | null => {
    const type = (file.type || '').toLowerCase();
    const okType = type.startsWith('image/') || type === 'application/pdf';
    if (!okType) return t('res.confirmation.invalidFile');
    if (file.size > MAX_RECEIPT_BYTES) return t('res.confirmation.invalidFile');
    return null;
  };

  const onFileChange = (file: File | null) => {
    setUploadDone(false);
    if (!file) {
      setSelectedFile(null);
      setFileError(null);
      return;
    }
    const err = validateFile(file);
    setFileError(err);
    setSelectedFile(err ? null : file);
  };

  const onUpload = async () => {
    if (!selectedFile || !reservation) return;
    const uri = reservation.links.self.replace(/\/$/, '') + '/payment-receipt';
    setUploading(true);
    setFileError(null);
    try {
      await uploadReceipt(uri, selectedFile);
      setUploadDone(true);
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      await queryClient.invalidateQueries({ queryKey: ['reservations', 'confirmation', id] });
    } catch {
      setFileError(t('res.confirmation.uploadError'));
    } finally {
      setUploading(false);
    }
  };

  const heading = t('res.confirmation.title');

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
                <p className="text-secondary">{t('res.detail.loading')}</p>
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
                            <p className="mb-0 fw-medium">{reservation.ownerCbu ?? '—'}</p>
                          </div>
                        </div>
                      </div>
                    </article>

                    {reservation.hasPaymentReceipt || uploadDone ? (
                      <p className="text-success small mb-0 text-center">
                        <i className="bi bi-check-circle-fill me-1" aria-hidden="true"></i>
                        {t('res.confirmation.uploadSuccess')}
                      </p>
                    ) : (
                      <>
                        <div className="d-flex align-items-stretch gap-2">
                          <label className="form-control d-flex align-items-center mb-0 flex-grow-1 min-w-0 position-relative">
                            <span className="text-truncate text-muted pe-1 flex-grow-1 min-w-0">
                              {selectedFile ? selectedFile.name : t('res.confirmation.chooseFile')}
                            </span>
                            <input
                              ref={fileInputRef}
                              type="file"
                              className="position-absolute top-0 start-0 w-100 h-100 opacity-0"
                              accept="image/*,application/pdf"
                              aria-label={t('res.confirmation.uploadAria')}
                              onChange={(e) => onFileChange(e.target.files?.[0] ?? null)}
                            />
                          </label>
                          <button
                            type="button"
                            className="btn btn-sm btn-primary flex-shrink-0 d-inline-flex align-items-center justify-content-center gap-1 px-2"
                            disabled={!selectedFile || uploading}
                            onClick={() => void onUpload()}
                          >
                            <i className="bi bi-cloud-arrow-up" aria-hidden="true"></i>
                            {t('res.confirmation.uploadReceipt')}
                          </button>
                        </div>
                        {fileError ? (
                          <div className="text-danger small mt-2" role="alert">
                            {fileError}
                          </div>
                        ) : null}
                      </>
                    )}
                  </section>

                  <div className="d-flex flex-wrap justify-content-center align-items-center gap-2">
                    <Link
                      to={myReservationDetail(id, { role: 'rider' })}
                      className="btn btn-sm btn-outline-primary px-3"
                    >
                      {t('res.confirmation.viewReservation')}
                    </Link>
                    {reservation.links.car ? (
                      <Link
                        to={carDetail(idFromUri(reservation.links.car) ?? '')}
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
