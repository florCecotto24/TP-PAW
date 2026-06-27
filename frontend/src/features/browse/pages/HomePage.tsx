import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import CarCarousel from '../components/CarCarousel';
import { useCheapestCars, useRecentCars } from '../hooks';

export default function BrowseHomePage() {
  const { t } = useTranslation();
  const cheapest = useCheapestCars();
  const recent = useRecentCars();

  return (
    <>
      <div className="hero-section w-100">
        <div className="hero-overlay">
          <div className="hero-text text-center text-white mb-4">
            <h1 className="fw-bold">{t('browse.home.hero.title')}</h1>
            <p className="lead">{t('browse.home.hero.subtitle')}</p>
            <p className="small mb-0 opacity-90">{t('browse.home.hero.note')}</p>
            <div className="mt-4">
              <Link to="/buscar" className="btn btn-light text-primary fw-semibold px-4 rounded-3 shadow-sm">
                {t('browse.home.searchCta')}
              </Link>
            </div>
          </div>
        </div>
      </div>

      <div className="container pt-5 pb-5 mb-5">
        <section className="carouselSection" id="cheapestVehiclesSection">
          {cheapest.isLoading ? (
            <p className="text-secondary" role="status">{t('app.loading')}</p>
          ) : null}
          {cheapest.isError ? (
            <div className="alert alert-danger" role="alert">{t('browse.home.loadError')}</div>
          ) : null}
          {cheapest.data && cheapest.data.length === 0 ? (
            <div className="alert-project" role="alert">{t('browse.home.empty')}</div>
          ) : null}
          {cheapest.data && cheapest.data.length > 0 ? (
            <CarCarousel
              id="cheapestVehiclesCarousel"
              cars={cheapest.data}
              title={t('browse.home.cheapest')}
              subtitle={t('browse.home.cheapestSubtitle')}
            />
          ) : null}
        </section>

        <section
          className="carouselSection mt-5 pt-5 border-top border-secondary-subtle"
          id="mostRecentVehiclesSection"
        >
          {recent.isLoading ? (
            <p className="text-secondary" role="status">{t('app.loading')}</p>
          ) : null}
          {recent.isError ? (
            <div className="alert alert-danger" role="alert">{t('browse.home.loadError')}</div>
          ) : null}
          {recent.data && recent.data.length === 0 ? (
            <div className="alert-project" role="alert">{t('browse.home.empty')}</div>
          ) : null}
          {recent.data && recent.data.length > 0 ? (
            <CarCarousel
              id="mostRecentVehiclesCarousel"
              cars={recent.data}
              title={t('browse.home.recent')}
              subtitle={t('browse.home.recentSubtitle')}
            />
          ) : null}
        </section>

        <section className="how-it-works-section mt-5 pt-5 border-top border-secondary-subtle" id="howItWorksSection">
          <h2 className="fw-semibold mb-2 text-center">{t('browse.home.howItWorks.title')}</h2>
          <p className="text-center mb-5 text-secondary small">{t('browse.home.howItWorks.subtitle')}</p>
          <div className="row g-4">
            {(['step1', 'step2', 'step3'] as const).map((step) => (
              <div key={step} className="col-12 col-md-4">
                <div className="how-it-works-step h-100 bg-white rounded-4 p-4 shadow-sm">
                  <span className="how-it-works-step__number">{t(`browse.home.howItWorks.${step}.label`)}</span>
                  <div className="how-it-works-step__icon">
                    <i
                      className={`bi bi-${step === 'step1' ? 'search' : step === 'step2' ? 'calendar-check' : 'car-front'}`}
                      aria-hidden="true"
                    ></i>
                  </div>
                  <h5 className="how-it-works-step__title">{t(`browse.home.howItWorks.${step}.title`)}</h5>
                  <p className="how-it-works-step__desc mb-0">{t(`browse.home.howItWorks.${step}.desc`)}</p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="features-section mt-5 pt-5 pb-5 border-top border-secondary-subtle text-center" id="whyChooseUsSection">
          <h2 className="mb-5 fw-semibold">{t('browse.home.features.title')}</h2>
          <div className="row g-4">
            {(['f1', 'f2', 'f3', 'f4'] as const).map((f) => (
              <div key={f} className="col-12 col-md-6 col-lg-3">
                <div className="feature-item bg-white rounded-4 p-4 h-100 shadow-sm">
                  <div
                    className="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle"
                    style={{
                      width: 72,
                      height: 72,
                      backgroundColor: 'var(--color-primary-soft, #eef4ff)',
                      color: 'var(--color-primary, #3b7be0)',
                    }}
                  >
                    <i
                      className={`bi bi-${f === 'f1' ? 'shield' : f === 'f2' ? 'cash-coin' : f === 'f3' ? 'clock' : 'star'} fs-2`}
                      aria-hidden="true"
                    ></i>
                  </div>
                  <h5 className="fw-semibold fs-6">{t(`browse.home.features.${f}.title`)}</h5>
                  <p className="text-muted small px-2 mb-0">{t(`browse.home.features.${f}.desc`)}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <section className="cta-banner text-center text-white py-5 w-100" style={{ backgroundColor: 'var(--color-primary, #3b7be0)' }}>
        <div className="container py-4 my-2">
          <h2 className="fw-bold mb-3">{t('browse.home.cta.title')}</h2>
          <p className="lead mb-4 opacity-90">{t('browse.home.cta.desc')}</p>
          <Link to="/publicar" className="btn btn-light text-primary fw-semibold px-4 rounded-3 shadow-sm">
            {t('nav.publish', { defaultValue: 'Publicar auto' })}
          </Link>
        </div>
      </section>
    </>
  );
}
