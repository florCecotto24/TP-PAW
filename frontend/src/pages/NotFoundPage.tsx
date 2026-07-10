import { useTranslation } from 'react-i18next';
import { Button } from '../components/ryden';
import { paths } from '../routes/paths';

export default function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <section className="container py-5">
      <div className="card bg-white rounded-4 shadow-sm border-0 p-4 p-md-5 ryden-not-found">
        <div className="ryden-not-found__icon" aria-hidden="true">
          <i className="bi bi-compass" />
        </div>
        <h1 className="h3 fw-bold mb-2">{t('notFound.title')}</h1>
        <p className="text-secondary mb-4">{t('notFound.message')}</p>
        <Button text={t('notFound.back')} href={paths.home} cssClass="btn-primary px-4 rounded-3" />
      </div>
    </section>
  );
}
