import { useCallback, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { collectionQueryPath } from '../../../api/apiDiscovery';
import { MediaTypes } from '../../../api/mediaTypes';
import { pageIndexFromParams, withPageIndex } from '../../../api/pageParam';
import { EmptyState, LoadingBlock } from '../../../components/ryden';
import { approveBrand, approveModel, rejectBrand, rejectModel } from '../api';
import AdminPageHeader from '../components/AdminPageHeader';
import AdminPagination from '../components/AdminPagination';
import type { BrandDto, ModelDto } from '../types';
import { useAdminErrorMessage } from '../useAdminErrorMessage';
import { usePagedList } from '../usePagedList';

export default function AdminCatalogPage() {
  const { t } = useTranslation();
  const errorMessage = useAdminErrorMessage();

  // La página (de marcas pendientes) vive en la URL (?page=N, 0-based como SearchPage)
  // -> bookmarkeable y resiste refresh.
  const [searchParams, setSearchParams] = useSearchParams();
  const pageIndex = pageIndexFromParams(searchParams);
  const goToPage = useCallback(
    (next: number) => setSearchParams(withPageIndex(searchParams, next)),
    [searchParams, setSearchParams],
  );
  const modelPageIndex = pageIndexFromParams(searchParams, 'modelsPage');
  const goToModelPage = useCallback(
    (next: number) => setSearchParams(withPageIndex(searchParams, next, 'modelsPage')),
    [searchParams, setSearchParams],
  );

  const brands = usePagedList<BrandDto>(
    collectionQueryPath('brands', { validated: 'false' }),
    MediaTypes.brand,
    pageIndex + 1,
  );
  const models = usePagedList<ModelDto>(
    collectionQueryPath('models', { validated: 'false' }),
    MediaTypes.model,
    modelPageIndex + 1,
  );
  const [busyLink, setBusyLink] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const runBrandAction = async (link: string, action: () => Promise<unknown>) => {
    setActionError(null);
    setBusyLink(link);
    try {
      await action();
      brands.reload();
      models.reload();
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setBusyLink(null);
    }
  };

  const runModelAction = async (link: string, action: () => Promise<unknown>) => {
    setActionError(null);
    setBusyLink(link);
    try {
      await action();
      models.reload();
      brands.reload();
    } catch (err) {
      setActionError(errorMessage(err));
    } finally {
      setBusyLink(null);
    }
  };

  const displayError =
    actionError
    ?? (brands.error ? errorMessage(brands.error) : null)
    ?? (models.error ? errorMessage(models.error) : null);

  return (
    <>
      <AdminPageHeader title={t('admin.catalog.title')} subtitle={t('admin.catalog.subtitle')} />

      {displayError ? <div className="alert alert-danger" role="alert">{displayError}</div> : null}

      <section className="mb-5">
        <h2 className="h5 fw-semibold mb-3">{t('admin.catalog.pendingBrandsTitle')}</h2>
        {brands.loading ? <LoadingBlock variant="inline" className="mb-3" /> : null}
        {!brands.loading && brands.items.length === 0 ? (
          <EmptyState icon="tags" title={t('admin.catalog.empty')} inCard />
        ) : null}
        {!brands.loading && brands.items.length > 0 ? (
          <div className="card border-0 shadow-sm bg-white">
            <ul className="list-group list-group-flush">
              {brands.items.map((brand) => {
                const busy = busyLink === brand.links.self;
                return (
                  <li
                    key={brand.links.self}
                    className="list-group-item d-flex flex-wrap align-items-center justify-content-between gap-2"
                  >
                    <span className="fw-semibold">{brand.name}</span>
                    <div className="d-flex gap-1">
                      <button
                        type="button"
                        className="btn btn-success btn-sm"
                        disabled={busy}
                        onClick={() => void runBrandAction(brand.links.self, () => approveBrand(brand.links.self))}
                      >
                        {t('admin.catalog.actions.approveBrand')}
                      </button>
                      <button
                        type="button"
                        className="btn btn-outline-danger btn-sm"
                        disabled={busy}
                        onClick={() => void runBrandAction(brand.links.self, () => rejectBrand(brand.links.self))}
                      >
                        {t('admin.catalog.actions.rejectBrand')}
                      </button>
                    </div>
                  </li>
                );
              })}
            </ul>
          </div>
        ) : null}
        <AdminPagination page={brands.page} currentPage={pageIndex} onPageChange={goToPage} />
      </section>

      <section>
        <h2 className="h5 fw-semibold mb-3">{t('admin.catalog.pendingModelsTitle')}</h2>
        {models.loading ? <LoadingBlock variant="inline" className="mb-3" /> : null}
        {!models.loading && models.items.length === 0 ? (
          <EmptyState icon="list-ul" title={t('admin.catalog.emptyModels')} inCard />
        ) : null}
        {!models.loading && models.items.length > 0 ? (
          <div className="card border-0 shadow-sm bg-white overflow-hidden">
            <div className="table-responsive">
              <table className="table table-hover align-middle mb-0 admin-table admin-table--catalog">
                <thead className="table-light">
                  <tr>
                    <th scope="col">{t('admin.catalog.brand')}</th>
                    <th scope="col">{t('admin.catalog.col.model')}</th>
                    <th scope="col">{t('admin.catalog.col.type')}</th>
                    <th scope="col" className="text-end admin-table__cell--wrap">{t('admin.users.col.actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {models.items.map((model) => {
                    const busy = busyLink === model.links.self;
                    const typeLabel = model.type ? t(`admin.cars.types.${model.type}`) : '—';
                    return (
                      <tr key={model.links.self}>
                        <td>{model.brandName ?? '—'}</td>
                        <td>{model.name}</td>
                        <td>{typeLabel}</td>
                        <td className="text-end admin-table__cell--wrap">
                          <div className="d-flex flex-wrap gap-1 justify-content-end">
                            <button
                              type="button"
                              className="btn btn-success btn-sm"
                              disabled={busy}
                              onClick={() =>
                                void runModelAction(model.links.self, () => approveModel(model.links.self))
                              }
                            >
                              {t('admin.catalog.actions.approveModel')}
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline-danger btn-sm"
                              disabled={busy}
                              onClick={() =>
                                void runModelAction(model.links.self, () => rejectModel(model.links.self))
                              }
                            >
                              {t('admin.catalog.actions.rejectModel')}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        ) : null}
        <AdminPagination page={models.page} currentPage={modelPageIndex} onPageChange={goToModelPage} />
      </section>
    </>
  );
}
