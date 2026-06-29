import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { MediaTypes } from '../../../api/mediaTypes';
import { fetchUserPublic, openAuthenticatedBinary } from '../api';
import AdminPageHeader from '../components/AdminPageHeader';
import { paths } from '../../../routes/paths';
import AdminPagination from '../components/AdminPagination';
import type { MessageDto } from '../types';
import { useAdminErrorMessage } from '../useAdminErrorMessage';
import { usePagedList } from '../usePagedList';

function formatDate(iso: string | undefined): string {
  if (!iso) return '';
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

export default function AdminReservationChatPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const errorMessage = useAdminErrorMessage();

  const messagesPath = id ? `/reservations/${id}/messages?page=1` : '';
  const list = usePagedList<MessageDto>(messagesPath, MediaTypes.message, [id]);

  const [senderNames, setSenderNames] = useState<Record<string, string>>({});

  const senderUris = useMemo(() => {
    const uris = new Set<string>();
    for (const msg of list.items) {
      const sender = msg.links.sender;
      if (sender) uris.add(sender);
    }
    return [...uris];
  }, [list.items]);

  useEffect(() => {
    let active = true;
    const missing = senderUris.filter((uri) => senderNames[uri] == null);
    for (const uri of missing) {
      fetchUserPublic(uri)
        .then((res) => {
          if (!active || !res.data) return;
          const name = `${res.data.forename} ${res.data.surname}`.trim();
          setSenderNames((prev) => ({ ...prev, [uri]: name || uri }));
        })
        .catch(() => {
          if (!active) return;
          setSenderNames((prev) => ({ ...prev, [uri]: t('admin.reservationChat.unknownSender') }));
        });
    }
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [senderUris, t]);

  const [attachmentError, setAttachmentError] = useState<string | null>(null);

  const onOpenAttachment = async (link: string | undefined) => {
    if (!link) return;
    setAttachmentError(null);
    const ok = await openAuthenticatedBinary(link);
    if (!ok) setAttachmentError(t('admin.reservationChat.attachmentFailed'));
  };

  const displayError = list.error ? errorMessage(list.error) : attachmentError;

  if (!id) {
    return <p className="text-secondary">{t('error.generic')}</p>;
  }

  return (
    <>
      <AdminPageHeader
        title={t('admin.reservationChat.title')}
        actions={(
          <Link to={paths.admin.reservations} className="btn btn-outline-secondary btn-sm">
            ← {t('admin.nav.reservations')}
          </Link>
        )}
      />

      {displayError ? <div className="alert alert-danger" role="alert">{displayError}</div> : null}
      {list.loading ? <p className="text-secondary" role="status">{t('app.loading')}</p> : null}

      {!list.loading && list.items.length === 0 ? (
        <p className="text-secondary">{t('admin.reservationChat.noMessages')}</p>
      ) : null}

      {!list.loading && list.items.length > 0 ? (
        <div className="card border-0 shadow-sm bg-white">
          <ul className="list-group list-group-flush">
            {list.items.map((message) => {
              const senderUri = message.links.sender ?? '';
              const senderLabel = senderNames[senderUri] ?? t('admin.reservationChat.unknownSender');
              return (
                <li key={message.links.self} className="list-group-item">
                  <div className="d-flex flex-wrap justify-content-between gap-2 mb-1">
                    <span className="fw-semibold">{senderLabel}</span>
                    <time className="text-secondary small" dateTime={message.createdAt}>
                      {formatDate(message.createdAt)}
                    </time>
                  </div>
                  <p className="mb-1">{message.body}</p>
                  {message.hasAttachment ? (
                    message.links.attachment ? (
                      <button
                        type="button"
                        className="btn btn-link btn-sm p-0"
                        onClick={() => void onOpenAttachment(message.links.attachment)}
                      >
                        <i className="bi bi-paperclip me-1" aria-hidden="true" />
                        {t('admin.reservationChat.attachment')}
                      </button>
                    ) : (
                      <span className="badge text-bg-light border">
                        <i className="bi bi-paperclip me-1" aria-hidden="true" />
                        {t('admin.reservationChat.attachment')}
                      </span>
                    )
                  ) : null}
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}

      <AdminPagination page={list.page} onGo={list.goTo} />
    </>
  );
}
