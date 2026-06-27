import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <section>
      <h1>{t('notFound.title')}</h1>
      <p>{t('notFound.message')}</p>
      <Link to="/">{t('notFound.back')}</Link>
    </section>
  );
}
