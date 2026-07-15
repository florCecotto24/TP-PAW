import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useParams, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatDateTime } from '../../../i18n/dateFormat';
import { MediaTypes } from '../../../api/mediaTypes';
import { pageIndexFromParams, withPageIndex } from '../../../api/pageParam';
import { fetchUserPublic } from '../api';
import { Avatar, ChatAttachmentPreview, EmptyState, LoadingBlock } from '../../../components/ryden';
import AdminPageHeader from '../components/AdminPageHeader';
import { paths } from '../../../routes/paths';
import type { AdminReservationChatLocationState } from '../../../routes/navigationState';
import AdminPagination from '../components/AdminPagination';
import type { MessageDto } from '../types';
import { useAdminErrorMessage } from '../useAdminErrorMessage';
import { usePagedList } from '../usePagedList';

type SenderProfile = {
  forename: string;
  surname: string;
};

function messageBody(msg: MessageDto): string {
  return (msg.body ?? '').trim();
}

export default function AdminReservationChatPage() {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const messagesLinkFromNav = (location.state as AdminReservationChatLocationState | null)?.messagesLink;
  const errorMessage = useAdminErrorMessage();

  // La página del chat vive en la URL (?page=N, 0-based como SearchPage) -> bookmarkeable
  // y resiste refresh (al recargar se pierde el link de nav y se cae al fallback por id).
  const [searchParams, setSearchParams] = useSearchParams();
  const pageIndex = pageIndexFromParams(searchParams);
  const goToPage = useCallback(
    (next: number) => setSearchParams(withPageIndex(searchParams, next)),
    [searchParams, setSearchParams],
  );

  const messagesPath = messagesLinkFromNav ?? (id ? `/reservations/${id}/messages` : '');
  const list = usePagedList<MessageDto>(messagesPath, MediaTypes.message, pageIndex + 1, [id]);

  const [senderProfiles, setSenderProfiles] = useState<Record<string, SenderProfile>>({});

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
    const missing = senderUris.filter((uri) => senderProfiles[uri] == null);
    for (const uri of missing) {
      fetchUserPublic(uri)
        .then((res) => {
          if (!active || !res.data) return;
          setSenderProfiles((prev) => ({
            ...prev,
            [uri]: {
              forename: res.data!.forename ?? '',
              surname: res.data!.surname ?? '',
            },
          }));
        })
        .catch(() => {
          if (!active) return;
          setSenderProfiles((prev) => ({
            ...prev,
            [uri]: { forename: t('admin.reservationChat.unknownSender'), surname: '' },
          }));
        });
    }
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [senderUris, t]);

  const displayError = list.error ? errorMessage(list.error) : null;

  if (!id) {
    return <p className="text-secondary">{t('error.generic')}</p>;
  }

  if (!messagesLinkFromNav && !list.loading && list.error) {
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
        <div className="alert alert-warning" role="alert">
          {t('admin.reservationChat.openFromList')}
        </div>
      </>
    );
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
      {list.loading ? <LoadingBlock variant="page" className="py-4" /> : null}

      {!list.loading && list.items.length === 0 ? (
        <EmptyState icon="chat-dots" title={t('admin.reservationChat.noMessages')} inCard />
      ) : null}

      {!list.loading && list.items.length > 0 ? (
        <div className="card border-0 shadow-sm bg-white">
          <ul className="list-group list-group-flush">
            {list.items.map((message) => {
              const senderUri = message.links.sender ?? '';
              const profile = senderProfiles[senderUri];
              const bodyText = messageBody(message);
              const attachmentUri = message.hasAttachment ? message.links.attachment : undefined;
              return (
                <li key={message.links.self} className="list-group-item">
                  <div className="d-flex gap-3">
                    <Avatar
                      forename={profile?.forename}
                      surname={profile?.surname}
                      className="reservation-chat-widget__avatar rounded-circle flex-shrink-0"
                      imgClassName="reservation-chat-widget__avatar rounded-circle flex-shrink-0"
                      placeholderClassName="reservation-chat-widget__avatar reservation-chat-header__avatar--placeholder rounded-circle flex-shrink-0 d-flex align-items-center justify-content-center"
                      colored
                      barePhoto
                      iconFallback
                    />
                    <div className="flex-grow-1 min-w-0">
                      <div className="d-flex flex-wrap justify-content-between gap-2 mb-1">
                        <span className="fw-semibold">
                          {profile
                            ? `${profile.forename} ${profile.surname}`.trim() || t('admin.reservationChat.unknownSender')
                            : t('admin.reservationChat.unknownSender')}
                        </span>
                        <time className="text-secondary small" dateTime={message.createdAt}>
                          {formatDateTime(message.createdAt, i18n.language)}
                        </time>
                      </div>
                      {attachmentUri ? (
                        <div className="mb-2">
                          <ChatAttachmentPreview attachmentUri={attachmentUri} />
                        </div>
                      ) : null}
                      {bodyText ? <p className="mb-0">{bodyText}</p> : null}
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}

      <AdminPagination page={list.page} currentPage={pageIndex} onPageChange={goToPage} />
    </>
  );
}
