import { useCallback, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MediaTypes } from '../../../api/mediaTypes';
import type { UserDto } from '../../../api/types';
import { useSessionStore } from '../../../session/sessionStore';
import { openAuthenticatedBinary, patchUser, userDocumentPath } from '../api';
import AdminPageHeader from '../components/AdminPageHeader';
import AdminPagination from '../components/AdminPagination';
import { idFromSelf } from '../types';
import { useAdminErrorMessage } from '../useAdminErrorMessage';
import { usePagedList } from '../usePagedList';

type RoleFilter = '' | 'user' | 'admin';
type BlockedFilter = '' | 'true' | 'false';

function UserDocRow({
  type,
  uploaded,
  validated,
  busy,
  onView,
  onToggleValidate,
}: {
  type: 'license' | 'identity';
  uploaded?: boolean;
  validated: boolean;
  busy: boolean;
  onView: () => void;
  onToggleValidate: (next: boolean) => void;
}) {
  const { t } = useTranslation();
  const label = t(`admin.users.docs.${type}`);
  let statusLabel = t('admin.users.docs.missing');
  let badgeClass = 'text-bg-secondary';
  if (uploaded && validated) {
    statusLabel = t('admin.users.docs.validated');
    badgeClass = 'text-bg-success';
  } else if (uploaded) {
    statusLabel = t('admin.users.docs.pending');
    badgeClass = 'text-bg-warning';
  }

  return (
    <div className="d-flex flex-wrap align-items-center gap-1 mb-1">
      <span className="small text-secondary">{label}:</span>
      <span className={`badge ${badgeClass}`}>{statusLabel}</span>
      {uploaded ? (
        <>
          <button
            type="button"
            className="btn btn-link btn-sm p-0"
            disabled={busy}
            onClick={onView}
          >
            {t('admin.users.docs.view')}
          </button>
          <button
            type="button"
            className="btn btn-link btn-sm p-0"
            disabled={busy}
            onClick={() => onToggleValidate(!validated)}
          >
            {validated ? t('admin.users.docs.invalidate') : t('admin.users.docs.validate')}
          </button>
        </>
      ) : null}
    </div>
  );
}

export default function AdminUsersPage() {
  const { t } = useTranslation();
  const errorMessage = useAdminErrorMessage();
  const currentUser = useSessionStore((s) => s.currentUser);
  const currentUserId = currentUser ? idFromSelf(currentUser.links) : '';

  const [roleFilter, setRoleFilter] = useState<RoleFilter>('');
  const [blockedFilter, setBlockedFilter] = useState<BlockedFilter>('');
  const [queryInput, setQueryInput] = useState('');
  const [queryApplied, setQueryApplied] = useState('');

  const [actionError, setActionError] = useState<string | null>(null);
  const [busySelf, setBusySelf] = useState<string | null>(null);

  const listPath = useMemo(() => {
    const params = new URLSearchParams({ page: '1' });
    if (roleFilter) params.set('role', roleFilter);
    if (blockedFilter) params.set('blocked', blockedFilter);
    if (queryApplied.trim()) params.set('q', queryApplied.trim());
    return `/users?${params.toString()}`;
  }, [roleFilter, blockedFilter, queryApplied]);

  const list = usePagedList<UserDto>(listPath, MediaTypes.userPrivate, [
    roleFilter,
    blockedFilter,
    queryApplied,
  ]);

  const runAction = useCallback(
    async (user: UserDto, action: () => Promise<unknown>) => {
      setActionError(null);
      setBusySelf(user.links.self);
      try {
        await action();
        list.reload();
      } catch (err) {
        setActionError(errorMessage(err));
      } finally {
        setBusySelf(null);
      }
    },
    [errorMessage, list],
  );

  const onApplyFilters = (e: React.FormEvent) => {
    e.preventDefault();
    setQueryApplied(queryInput);
  };

  const onViewDocument = async (user: UserDto, type: 'license' | 'identity') => {
    const ok = await openAuthenticatedBinary(userDocumentPath(user.links.self, type));
    if (!ok) setActionError(t('admin.users.docs.openFailed'));
  };

  const displayError = actionError ?? (list.error ? errorMessage(list.error) : null);

  return (
    <>
      <AdminPageHeader title={t('admin.users.title')} subtitle={t('admin.users.subtitle')} />

      <form className="row g-2 align-items-end mb-3" onSubmit={onApplyFilters}>
        <div className="col-md-4">
          <label className="form-label small mb-1" htmlFor="adminUserSearch">
            {t('admin.users.filter.search')}
          </label>
          <input
            id="adminUserSearch"
            type="search"
            className="form-control form-control-sm"
            placeholder={t('admin.users.filter.searchPlaceholder')}
            value={queryInput}
            onChange={(e) => setQueryInput(e.target.value)}
          />
        </div>
        <div className="col-md-3">
          <label className="form-label small mb-1" htmlFor="adminUserRole">
            {t('admin.users.filter.role')}
          </label>
          <select
            id="adminUserRole"
            className="form-select form-select-sm"
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value as RoleFilter)}
          >
            <option value="">{t('admin.users.filter.roleAll')}</option>
            <option value="user">{t('admin.users.roles.user')}</option>
            <option value="admin">{t('admin.users.roles.admin')}</option>
          </select>
        </div>
        <div className="col-md-3">
          <label className="form-label small mb-1" htmlFor="adminUserBlocked">
            {t('admin.users.filter.status')}
          </label>
          <select
            id="adminUserBlocked"
            className="form-select form-select-sm"
            value={blockedFilter}
            onChange={(e) => setBlockedFilter(e.target.value as BlockedFilter)}
          >
            <option value="">{t('admin.users.filter.statusAll')}</option>
            <option value="false">{t('admin.users.filter.statusActive')}</option>
            <option value="true">{t('admin.users.filter.statusBlocked')}</option>
          </select>
        </div>
        <div className="col-md-2">
          <button type="submit" className="btn btn-outline-primary btn-sm w-100">
            {t('admin.users.filter.apply')}
          </button>
        </div>
      </form>

      {displayError ? <div className="alert alert-danger" role="alert">{displayError}</div> : null}
      {list.loading ? <p className="text-secondary" role="status">{t('app.loading')}</p> : null}

      {!list.loading && list.items.length === 0 ? (
        <p className="text-secondary">{t('admin.users.empty')}</p>
      ) : null}

      {!list.loading && list.items.length > 0 ? (
        <div className="card border-0 shadow-sm bg-white overflow-hidden">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th scope="col">{t('admin.users.col.name')}</th>
                  <th scope="col">{t('admin.users.col.email')}</th>
                  <th scope="col">{t('admin.users.col.role')}</th>
                  <th scope="col">{t('admin.users.col.status')}</th>
                  <th scope="col">{t('admin.users.col.docs')}</th>
                  <th scope="col" className="text-end">{t('admin.users.col.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {list.items.map((user) => {
                  const userId = idFromSelf(user.links);
                  const isMe = userId === currentUserId;
                  const busy = busySelf === user.links.self;
                  const roleKey = user.role === 'admin' ? 'admin' : 'user';
                  const statusKey = user.blocked ? 'blocked' : 'active';
                  return (
                    <tr key={user.links.self}>
                      <td>
                        {user.forename} {user.surname}
                        {isMe ? (
                          <span className="badge text-bg-secondary ms-2">{t('admin.users.me')}</span>
                        ) : null}
                      </td>
                      <td>{user.email ?? '—'}</td>
                      <td>{t(`admin.users.roles.${roleKey}`)}</td>
                      <td>{t(`admin.users.statuses.${statusKey}`)}</td>
                      <td className="small" style={{ minWidth: 200 }}>
                        <UserDocRow
                          type="license"
                          uploaded={user.licenseUploaded}
                          validated={user.licenseValidated}
                          busy={busy}
                          onView={() => void onViewDocument(user, 'license')}
                          onToggleValidate={(next) =>
                            void runAction(user, () =>
                              patchUser(user.links.self, { licenseValidated: next }),
                            )
                          }
                        />
                        <UserDocRow
                          type="identity"
                          uploaded={user.identityUploaded}
                          validated={user.identityValidated}
                          busy={busy}
                          onView={() => void onViewDocument(user, 'identity')}
                          onToggleValidate={(next) =>
                            void runAction(user, () =>
                              patchUser(user.links.self, { identityValidated: next }),
                            )
                          }
                        />
                      </td>
                      <td className="text-end">
                        <div className="d-flex flex-wrap gap-1 justify-content-end">
                          {user.role !== 'admin' && !isMe ? (
                            <button
                              type="button"
                              className="btn btn-outline-primary btn-sm"
                              disabled={busy}
                              onClick={() =>
                                void runAction(user, () =>
                                  patchUser(user.links.self, { role: 'admin' }),
                                )
                              }
                            >
                              {t('admin.users.actions.promote')}
                            </button>
                          ) : null}
                          {user.role === 'admin' && !isMe ? (
                            <button
                              type="button"
                              className="btn btn-outline-warning btn-sm"
                              disabled={busy}
                              onClick={() =>
                                void runAction(user, () =>
                                  patchUser(user.links.self, { role: 'user' }),
                                )
                              }
                            >
                              {t('admin.users.actions.demote')}
                            </button>
                          ) : null}
                          {!user.blocked && !isMe ? (
                            <button
                              type="button"
                              className="btn btn-outline-danger btn-sm"
                              disabled={busy}
                              onClick={() =>
                                void runAction(user, () =>
                                  patchUser(user.links.self, { blocked: true }),
                                )
                              }
                            >
                              {t('admin.users.actions.block')}
                            </button>
                          ) : null}
                          {user.blocked ? (
                            <button
                              type="button"
                              className="btn btn-outline-success btn-sm"
                              disabled={busy}
                              onClick={() =>
                                void runAction(user, () =>
                                  patchUser(user.links.self, { blocked: false }),
                                )
                              }
                            >
                              {t('admin.users.actions.unblock')}
                            </button>
                          ) : null}
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

      <AdminPagination page={list.page} onGo={list.goTo} />
    </>
  );
}
