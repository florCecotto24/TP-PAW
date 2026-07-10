import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { Button } from '../../../components/ryden';
import HomeCarouselBlock from '../components/HomeCarouselBlock';
import ExploreSearchForm from '../components/ExploreSearchForm';
import { useCheapestCars, useRecentCars } from '../hooks';
import { parseHomeCarouselPages } from '../homeCarouselPages';
import { useHomePageAnimations } from '../useHomePageAnimations';
import { appBasePath } from '../../../appBasePath';
import { paths } from '../../../routes/paths';

const HERO_PHOTO_URL = `${appBasePath()}/assets/images/homepage-photo.jpg`;

export default function BrowseHomePage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const { cheapestPage, recentPage } = useMemo(
    () => parseHomeCarouselPages(searchParams),
    [searchParams],
  );

  const cheapest = useCheapestCars(cheapestPage);
  const recent = useRecentCars(recentPage);

  const cheapestItems = cheapest.data?.items ?? [];
  const recentItems = recent.data?.items ?? [];

  useHomePageAnimations(
    cheapestItems.length + recentItems.length + Number(!cheapest.isLoading) + Number(!recent.isLoading),
  );

  return (
    <>
      <div
        className="hero-section w-100"
        style={{ backgroundImage: `url('${HERO_PHOTO_URL}')` }}
      >
        <div className="hero-overlay pb-2">
          <div className="hero-text mb-4">
            <h1 className="fw-bold">{t('home.hero.title')}</h1>
            <p className="lead">{t('home.hero.subtitle')}</p>
            <p className="small mb-0">{t('home.hero.argentinaReservationsNote')}</p>
          </div>
        </div>
      </div>

      <ExploreSearchForm
        formId="homeSearchForm"
        formClass="search-menu sticky-top w-100"
        actionPath={paths.search}
        clearFiltersHref={paths.home}
        allowFlexibleSearch
      />

      <div className="container mt-5">
        <HomeCarouselBlock
          sectionId="cheapestVehiclesSection"
          carouselId="cheapestVehiclesCarousel"
          title={t('home.cheapest.title')}
          subtitle={t('home.cheapest.subtitle')}
          items={cheapestItems}
          isLoading={cheapest.isLoading}
          isError={cheapest.isError}
        />

        <HomeCarouselBlock
          sectionId="mostRecentVehiclesSection"
          carouselId="mostRecentVehiclesCarousel"
          sectionClassName="carouselSection mt-5 pt-5 border-top border-secondary-subtle animate-on-scroll"
          title={t('home.recent.title')}
          subtitle={t('home.recent.subtitle')}
          items={recentItems}
          isLoading={recent.isLoading}
          isError={recent.isError}
        />

        <section
          className="how-it-works-section mt-5 pt-5 border-top border-secondary-subtle animate-on-scroll"
          id="howItWorksSection"
        >
          <h2 className="fw-semibold mb-2 text-center">{t('home.howItWorks.title')}</h2>
          <p className="text-center mb-5" style={{ color: 'var(--color-text-muted)', fontSize: '0.95rem' }}>
            {t('home.howItWorks.subtitle')}
          </p>
          <div className="row g-4 animate-stagger-parent">
            {(['step1', 'step2', 'step3'] as const).map((step) => (
              <div key={step} className="col-12 col-md-4 animate-stagger-child">
                <div className="how-it-works-step h-100">
                  <span className="how-it-works-step__number">{t(`home.howItWorks.${step}.label`)}</span>
                  <div className="how-it-works-step__icon">
                    <i
                      className={`bi bi-${step === 'step1' ? 'search' : step === 'step2' ? 'calendar-check' : 'car-front'}`}
                      aria-hidden="true"
                    ></i>
                  </div>
                  <h5 className="how-it-works-step__title">{t(`home.howItWorks.${step}.title`)}</h5>
                  <p className="how-it-works-step__desc">{t(`home.howItWorks.${step}.desc`)}</p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section
          className="features-section mt-5 pt-5 pb-5 border-top border-secondary-subtle text-center animate-on-scroll"
          id="whyChooseUsSection"
        >
          <h2 className="mb-5 fw-semibold">{t('home.features.title')}</h2>
          <div className="row g-4 animate-stagger-parent">
            {(
              [
                { key: 'safe', icon: 'shield' },
                { key: 'price', icon: 'cash-coin' },
                { key: 'hours', icon: 'clock' },
                { key: 'premium', icon: 'star' },
              ] as const
            ).map(({ key, icon }) => (
              <div key={key} className="col-12 col-md-6 col-lg-3 feature-item animate-stagger-child">
                <div
                  className="mb-3 d-inline-flex align-items-center justify-content-center rounded-circle feature-icon-wrap"
                  style={{
                    width: 72,
                    height: 72,
                    backgroundColor: 'var(--color-primary-soft, #eef4ff)',
                    color: 'var(--color-primary, #3b7be0)',
                  }}
                >
                  <i className={`bi bi-${icon} fs-2`} aria-hidden="true"></i>
                </div>
                <h5 className="fw-semibold fs-6">{t(`home.features.${key}.title`)}</h5>
                <p className="text-muted small px-3">{t(`home.features.${key}.desc`)}</p>
              </div>
            ))}
          </div>
        </section>
      </div>

      <section
        className="cta-banner animate-on-scroll text-center text-white py-5 w-100"
      >
        <div className="container py-4 my-2">
          <h2 className="fw-bold mb-3">{t('home.cta.title')}</h2>
          <p className="lead mb-4" style={{ fontSize: '1.1rem', opacity: 0.9 }}>
            {t('home.cta.desc')}
          </p>
          <Button
            text={t('home.cta.button')}
            size="lg"
            href={paths.publishCar}
            cssClass="btn-light text-primary fw-semibold px-4 rounded-3 shadow-sm"
          />
        </div>
      </section>
    </>
  );
}
