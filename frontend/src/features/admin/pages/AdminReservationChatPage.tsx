import { useCallback } from 'react';
import { useLocation, useParams, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { formatDateTime } from '../../../i18n/dateFormat';
import { MediaTypes } from '../../../api/mediaTypes';
import { pageIndexFromParams, withPageIndex } from '../../../api/pageParam';
import { Avatar, ChatAttachmentPreview, EmptyState, LoadingBlock } from '../../../components/ryden';
import AdminPageHeader from '../components/AdminPageHeader';
import { paths } from '../../../routes/paths';
import type { AdminReservationChatLocationState } from '../../../routes/navigationState';
import { resolveResourceUri } from '../../../api/resourceUri';
import { getReservation } from '../../reservations/api';
import AdminPagination from '../components/AdminPagination';
import type { MessageDto } from '../types';
import { useAdminErrorMessage } from '../useAdminErrorMessage';
import { usePagedList } from '../usePagedList';

function messageBody(msg: MessageDto): string {
  return (msg.body ?? '').trim();
}

function senderDisplayName(msg: MessageDto, unknownLabel: string): string {
  const name = `${msg.senderForename ?? ''} ${msg.senderSurname ?? ''}`.trim();
  return name || unknownLabel;
}

export default function AdminReservationChatPage() {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const messagesLinkFromNav = (location.state as AdminReservationChatLocationState | null)?.messagesLink;
  const reservationSelfFromNav = (location.state as AdminReservationChatLocationState | null)?.reservationSelf;
  const [searchParams, setSearchParams] = useSearchParams();
  const errorMessage = useAdminErrorMessage();

  const reservationSelf = resolveResourceUri({
    stateUri: reservationSelfFromNav,
    querySelf: searchParams.get('self'),
    routeId: id,
    collection: 'reservations',
  });

  const messagesLinkQuery = useQuery({
    queryKey: ['admin', 'reservation-chat', 'messages-link', messagesLinkFromNav, reservationSelf],
    queryFn: async () => {
      if (messagesLinkFromNav) return messagesLinkFromNav;
      if (!reservationSelf) return null;
      const res = await getReservation(reservationSelf);
      const link = res.data?.links?.messages;
      if (!link) throw new Error('missing messages link');
      return link;
    },
    enabled: Boolean(messagesLinkFromNav || reservationSelf),
  });

  const messagesPath = messagesLinkQuery.data ?? '';

  // La página del chat vive en la URL (?page=N, 0-based como SearchPage) -> bookmarkeable
  // y resiste refresh: sin state se resuelve messages vía GET reserva (self canónico + links.messages).
  const pageIndex = pageIndexFromParams(searchParams);
  const goToPage = useCallback(
    (next: number) => setSearchParams(withPageIndex(searchParams, next)),
    [searchParams, setSearchParams],
  );

  const list = usePagedList<MessageDto>(messagesPath, MediaTypes.message, pageIndex + 1, [
    messagesPath,
  ]);

  const displayError = list.error ? errorMessage(list.error) : null;

  const resolvingMessages = messagesLinkQuery.isLoading;
  const messagesResolveFailed = messagesLinkQuery.isError;

  if (!id) {
    return <p className="text-secondary">{t('error.generic')}</p>;
  }

  if (messagesResolveFailed && !resolvingMessages) {
    return (
      <>
        <AdminPageHeader
          title={t('admin.reservationChat.title')}
          midLabel={t('admin.reservations.title')}
          midHref={paths.admin.reservations}
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
        midLabel={t('admin.reservations.title')}
        midHref={paths.admin.reservations}
      />
      {displayError ? <div className="alert alert-danger" role="alert">{displayError}</div> : null}
      {resolvingMessages || list.loading ? <LoadingBlock variant="page" className="py-4" /> : null}

      {!resolvingMessages && !list.loading && list.items.length === 0 ? (
        <EmptyState icon="chat-dots" title={t('admin.reservationChat.noMessages')} inCard />
      ) : null}

      {!resolvingMessages && !list.loading && list.items.length > 0 ? (
        <div className="card border-0 shadow-sm bg-white">
          <ul className="list-group list-group-flush">
            {list.items.map((message) => {
              const bodyText = messageBody(message);
              const attachmentUri = message.hasAttachment ? message.links.attachment : undefined;
              const forename = message.senderForename ?? '';
              const surname = message.senderSurname ?? '';
              return (
                <li key={message.links.self} className="list-group-item">
                  <div className="d-flex gap-3">
                    <Avatar
                      forename={forename}
                      surname={surname}
                      className="reviewer-avatar flex-shrink-0"
                      imgClassName="reviewer-avatar flex-shrink-0"
                      colored
                      barePhoto
                    />
                    <div className="flex-grow-1 min-w-0">
                      <div className="d-flex flex-wrap justify-content-between gap-2 mb-1">
                        <span className="fw-semibold">
                          {senderDisplayName(message, t('admin.reservationChat.unknownSender'))}
                        </span>
                        <time className="text-secondary small" dateTime={message.createdAt}>
                          {formatDateTime(message.createdAt, i18n.language)}
                        </time>
                      </div>
                      {attachmentUri ? (
                        <div className="mb-2">
                          <ChatAttachmentPreview
                            attachmentUri={attachmentUri}
                            attachmentMeta={message.attachment ?? undefined}
                          />
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
