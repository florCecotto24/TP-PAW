import { useTranslation } from 'react-i18next';

export interface CounterpartyProfileHeaderProps {
  forename: string;
  surname: string;
  about?: string | null;
  memberSinceDisplay?: string | null;
  profileImageUrl?: string | null;
  averageRating?: number | null;
  reviewCount?: number | null;
  licenseValidated?: boolean;
  identityValidated?: boolean;
  ratingFloor?: number;
}

function StarRow({ averageRating, ratingFloor }: { averageRating: number; ratingFloor: number }) {
  const ratingFraction = averageRating - ratingFloor;
  return (
    <>
      {Array.from({ length: 5 }, (_, i) => {
        const star = i + 1;
        if (star <= ratingFloor) {
          return <i key={star} className="bi bi-star-fill text-warning" aria-hidden="true"></i>;
        }
        if (star === ratingFloor + 1 && ratingFraction >= 0.4 && ratingFraction <= 0.6) {
          return <i key={star} className="bi bi-star-half text-warning" aria-hidden="true"></i>;
        }
        return <i key={star} className="bi bi-star text-secondary-subtle" aria-hidden="true"></i>;
      })}
    </>
  );
}

/** Espejo de {@code ryden:counterpartyProfileHeader} (+ sección documentos del mismo tag). */
export default function CounterpartyProfileHeader({
  forename,
  surname,
  about,
  memberSinceDisplay,
  profileImageUrl,
  averageRating,
  reviewCount,
  licenseValidated = false,
  identityValidated = false,
  ratingFloor = 0,
}: CounterpartyProfileHeaderProps) {
  const { t } = useTranslation();
  const initials = `${forename[0] ?? ''}${surname[0] ?? ''}`;

  return (
    <>
      <section className="card border-0 shadow-sm rounded-4 counterparty-section-card counterparty-header-card">
        <div className="card-body p-4">
          <div className="d-flex flex-column flex-md-row align-items-start gap-4">
            <div className="counterparty-avatar">
              {profileImageUrl ? (
                <img
                  src={profileImageUrl}
                  alt={`${forename} ${surname}`}
                  className="counterparty-avatar__img"
                />
              ) : (
                <div className="counterparty-avatar__placeholder">
                  <span>{initials}</span>
                </div>
              )}
            </div>
            <div className="flex-grow-1 min-w-0">
              <div className="d-flex flex-wrap align-items-center gap-2">
                <h1 className="h4 fw-semibold mb-0 ryden-text-break">
                  {forename} {surname}
                </h1>
              </div>
              <div className="d-flex align-items-center gap-2 mt-2">
                {averageRating != null ? (
                  <>
                    <span className="counterparty-rating-value">{averageRating.toFixed(1)}</span>
                    <div
                      className="d-inline-flex align-items-center gap-1"
                      aria-label={t('counterpartyProfile.rating.ariaLabel')}
                    >
                      <StarRow averageRating={averageRating} ratingFloor={ratingFloor} />
                    </div>
                    {reviewCount != null && reviewCount > 0 ? (
                      <span className="text-secondary small">
                        {t('counterpartyProfile.reviews.count', { count: reviewCount })}
                      </span>
                    ) : null}
                  </>
                ) : (
                  <span className="counterparty-rating-value">
                    {t('counterpartyProfile.reviews.emptyShort')}
                  </span>
                )}
              </div>
              {about ? (
                <p className="counterparty-about mt-3 mb-2 ryden-multiline-plaintext">{about}</p>
              ) : (
                <p className="counterparty-about mt-3 mb-2">{t('counterpartyProfile.about.empty')}</p>
              )}
              {memberSinceDisplay ? (
                <p className="counterparty-member-since mb-0">
                  <span className="text-secondary">{t('profile.memberSince')}</span>{' '}
                  <span className="fw-semibold">{memberSinceDisplay}</span>
                </p>
              ) : null}
            </div>
          </div>
        </div>
      </section>

      <section className="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4 mt-4">
        <div className="card-body p-4">
          <h2 className="h5 fw-semibold mb-3">{t('profile.documents.sectionTitle')}</h2>
          <ul className="list-unstyled mb-0">
            <li className="mb-2">
              <i
                className={`bi bi-${licenseValidated ? 'check-circle-fill text-success' : 'x-circle-fill text-danger'}`}
                aria-hidden="true"
              ></i>
              <span className="visually-hidden">
                {t(
                  licenseValidated
                    ? 'profile.documents.status.validated'
                    : 'profile.documents.status.notValidated',
                )}
              </span>
              <span className="fw-semibold ms-1">{t('profile.documents.license')}</span>
            </li>
            <li>
              <i
                className={`bi bi-${identityValidated ? 'check-circle-fill text-success' : 'x-circle-fill text-danger'}`}
                aria-hidden="true"
              ></i>
              <span className="visually-hidden">
                {t(
                  identityValidated
                    ? 'profile.documents.status.validated'
                    : 'profile.documents.status.notValidated',
                )}
              </span>
              <span className="fw-semibold ms-1">{t('profile.documents.identity')}</span>
            </li>
          </ul>
        </div>
      </section>
    </>
  );
}
