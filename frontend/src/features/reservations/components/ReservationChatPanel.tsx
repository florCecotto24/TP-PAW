import { Fragment, useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ApiError } from '../../../api/client';
import { MediaTypes } from '../../../api/mediaTypes';
import { formatTime } from '../../../i18n/dateFormat';
import { sessionClient } from '../../../session/sessionStore';
import { idFromUri, listMessages, listMessagesLatestPage } from '../api';
import {
  CHAT_FILE_ACCEPT,
  formatFileSize,
  getChatMaxAttachmentMb,
  getChatMessageMaxLength,
  validateChatFile,
} from '../chatAttachment';
import { formatDayLabel, groupMessagesByDay, latestMessageId, mergeMessages } from '../chatLog';
import { sendChatMessage } from '../chatUpload';
import { isChatAvailable } from '../reservationChat';
import { useReservationChatStickyDay } from '../useReservationChatStickyDay';
import { useCurrentUser } from '../useCurrentUser';
import type { MessageDto, ReservationDto } from '../types';
import { ChatAttachmentPreview, Avatar } from '../../../components/ryden';

const POLL_INTERVAL_MS = 5000;

interface PageLinks {
  prev?: string;
  next?: string;
}

function messageBody(msg: MessageDto): string {
  return (msg.body ?? '').trim();
}

function ChatMessageBubble({
  msg,
  mine,
  locale,
}: {
  msg: MessageDto;
  mine: boolean;
  locale: string;
}) {
  const bodyText = messageBody(msg);
  const attachmentUri = msg.hasAttachment ? msg.links.attachment : undefined;
  const mediaOnly = attachmentUri != null && !bodyText;
  const [visualOnly, setVisualOnly] = useState(false);

  const bubbleClass = [
    'reservation-chat__bubble',
    mine ? 'reservation-chat__bubble--mine' : 'reservation-chat__bubble--theirs',
    attachmentUri ? 'reservation-chat__bubble--has-attachment' : '',
    mediaOnly ? 'reservation-chat__bubble--media-only' : '',
    mediaOnly && visualOnly ? 'reservation-chat__bubble--visual-only' : '',
  ]
    .filter(Boolean)
    .join(' ');

  const meta = (
    <div className="reservation-chat__meta">
      <span className="reservation-chat__time">{formatTime(msg.createdAt, locale)}</span>
      {mine ? (
        <span
          className={`reservation-chat__receipt bi bi-check2-all${msg.seen ? ' reservation-chat__receipt--seen' : ''}`}
          aria-hidden="true"
        />
      ) : null}
    </div>
  );

  return (
    <div
      className={`reservation-chat__message mb-2${mine ? ' reservation-chat__message--mine' : ''}`}
      data-message-id={idFromUri(msg.links.self) ?? undefined}
    >
      <div className={bubbleClass}>
        {attachmentUri ? (
          <ChatAttachmentPreview attachmentUri={attachmentUri} onVisualKind={setVisualOnly} />
        ) : null}
        {bodyText ? (
          attachmentUri ? (
            <div className="reservation-chat__bubble-footer">
              <span className="reservation-chat__bubble-text reservation-chat__attachment-caption">{bodyText}</span>
              {meta}
            </div>
          ) : (
            <>
              <span className="reservation-chat__bubble-text">{bodyText}</span>
              {meta}
            </>
          )
        ) : (
          meta
        )}
      </div>
    </div>
  );
}

export interface CounterpartyChatInfo {
  name: string;
  forename?: string;
  surname?: string;
  avatarUrl?: string | null;
}

export interface ReservationChatPanelProps {
  reservation: ReservationDto;
  expanded: boolean;
  onExpandedChange: (expanded: boolean) => void;
  counterparty?: CounterpartyChatInfo;
}

function ChatAvatar({
  avatarUrl,
  forename,
  surname,
  className,
}: {
  avatarUrl?: string | null;
  forename?: string;
  surname?: string;
  className: string;
}) {
  return (
    <Avatar
      src={avatarUrl}
      forename={forename}
      surname={surname}
      className={className}
      imgClassName={className}
      placeholderClassName={`${className} reservation-chat-header__avatar--placeholder d-flex align-items-center justify-content-center`}
      barePhoto
      iconFallback
    />
  );
}

export default function ReservationChatPanel({
  reservation,
  expanded,
  onExpandedChange,
  counterparty,
}: ReservationChatPanelProps) {
  const { t, i18n } = useTranslation();
  const { id: myId } = useCurrentUser();
  const messagesUri = reservation.links.messages;
  const chatAvailable = isChatAvailable(reservation);

  const [messages, setMessages] = useState<MessageDto[]>([]);
  const [pageLinks, setPageLinks] = useState<PageLinks>({});
  const [body, setBody] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(true);
  const [paging, setPaging] = useState(false);
  const [sending, setSending] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [sendError, setSendError] = useState<string | null>(null);
  const [dropOverlayVisible, setDropOverlayVisible] = useState(false);

  const messagesRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const dragDepthRef = useRef(0);
  const onLastPage = !pageLinks.next;

  const hasMessages = !loading && messages.length > 0;
  const { stickyDayKey, dayBarScrolling, showDayBar, registerSeparator, syncStickyToLastDay } =
    useReservationChatStickyDay(messagesRef, hasMessages);

  const loadLatest = useCallback(async () => {
    if (!messagesUri) return;
    try {
      const res = await listMessagesLatestPage(messagesUri);
      setMessages(res.data ?? []);
      setPageLinks({ prev: res.page.prev, next: res.page.next });
      setLoadError(null);
    } catch (err) {
      if (err instanceof ApiError && err.code === 'reservation.chat.notAvailable') {
        setLoadError(null);
        setMessages([]);
        return;
      }
      setLoadError(t('res.chat.loadError'));
    } finally {
      setLoading(false);
    }
  }, [messagesUri, t]);

  useEffect(() => {
    if (!chatAvailable || !messagesUri) {
      setLoading(false);
      return;
    }
    setLoading(true);
    void loadLatest();
  }, [chatAvailable, loadLatest, messagesUri]);

  useEffect(() => {
    if (!messagesUri || !onLastPage || !chatAvailable) return;
    const timer = window.setInterval(() => {
      setMessages((prev) => {
        const after = latestMessageId(prev);
        if (after > 0) {
          void listMessages(messagesUri, { afterId: after })
            .then((res) => {
              if (res.data?.length) {
                setMessages((cur) => mergeMessages(cur, res.data as MessageDto[]));
              }
            })
            .catch(() => {
              /* polling en segundo plano */
            });
        }
        return prev;
      });
    }, POLL_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [messagesUri, onLastPage, chatAvailable]);

  useEffect(() => {
    if (onLastPage) {
      syncStickyToLastDay();
    }
  }, [messages.length, onLastPage, syncStickyToLastDay]);

  useEffect(() => {
    if (!expanded || !messagesRef.current) return;
    const el = messagesRef.current;
    requestAnimationFrame(() => {
      el.scrollTop = el.scrollHeight;
    });
  }, [expanded, messages.length]);

  const clearPendingFile = () => {
    setFile(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const setPendingFile = (next: File | null) => {
    if (!next) {
      clearPendingFile();
      return;
    }
    const validation = validateChatFile(next);
    if (validation === 'tooLarge') {
      setSendError(t('res.chat.tooLarge', { max: getChatMaxAttachmentMb() }));
      return;
    }
    if (validation === 'invalidType') {
      setSendError(t('res.chat.invalidType'));
      return;
    }
    setSendError(null);
    setFile(next);
  };

  const showDropOverlay = (visible: boolean) => {
    setDropOverlayVisible(visible);
  };

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragDepthRef.current += 1;
    if (dragDepthRef.current === 1) showDropOverlay(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragDepthRef.current = Math.max(0, dragDepthRef.current - 1);
    if (dragDepthRef.current === 0) showDropOverlay(false);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.dataTransfer) e.dataTransfer.dropEffect = 'copy';
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragDepthRef.current = 0;
    showDropOverlay(false);
    const dropped = e.dataTransfer?.files?.[0];
    if (dropped) setPendingFile(dropped);
  };

  const goToPage = async (link: string | undefined) => {
    if (!link || paging) return;
    setPaging(true);
    try {
      const res = await sessionClient.follow<MessageDto[]>(link, { accept: MediaTypes.message });
      setMessages(res.data ?? []);
      setPageLinks({ prev: res.page.prev, next: res.page.next });
      setLoadError(null);
    } catch {
      setLoadError(t('res.chat.loadError'));
    } finally {
      setPaging(false);
    }
  };

  const onSend = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!messagesUri || sending || uploading) return;
    const trimmed = body.trim();
    if (!trimmed && !file) return;

    if (file) {
      const validation = validateChatFile(file);
      if (validation === 'tooLarge') {
        setSendError(t('res.chat.tooLarge', { max: getChatMaxAttachmentMb() }));
        return;
      }
      if (validation === 'invalidType') {
        setSendError(t('res.chat.invalidType'));
        return;
      }
    }

    setSending(true);
    setSendError(null);
    const fileToSend = file;
    const caption = trimmed;
    clearPendingFile();
    setBody('');

    if (fileToSend) {
      setUploading(true);
      setUploadProgress(0);
      setSendError(t('res.chat.uploading'));
    }

    try {
      const res = await sendChatMessage(messagesUri, caption, fileToSend, {
        onProgress: fileToSend ? setUploadProgress : undefined,
      });
      setSendError(null);
      if (onLastPage && res.data) {
        setMessages((prev) => mergeMessages(prev, [res.data as MessageDto]));
      } else {
        await loadLatest();
      }
    } catch (err) {
      if (err instanceof Error && err.message === 'timeout') {
        setSendError(t('res.chat.uploadTimeout'));
      } else if (err instanceof ApiError) {
        setSendError(err.message || t('res.chat.sendError'));
      } else {
        setSendError(t('res.chat.sendError'));
      }
      setBody(caption);
      if (fileToSend) setFile(fileToSend);
    } finally {
      setSending(false);
      setUploading(false);
      setUploadProgress(null);
    }
  };

  if (!chatAvailable || !messagesUri) {
    return null;
  }

  const dayGroups = groupMessagesByDay(messages);
  const composerDisabled = sending || uploading;
  const showEmptyState = !loading && messages.length === 0;
  const displayName = counterparty?.name?.trim() || t('res.chat.title');

  const chatShell = (
    <div
      className="reservation-chat reservation-chat-page reservation-chat-shell p-3 reservation-chat__drop-zone"
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
    >
      <div
        className={`reservation-chat__drop-overlay${dropOverlayVisible ? '' : ' d-none'}`}
        aria-hidden={!dropOverlayVisible}
      >
        <span className="reservation-chat__drop-overlay-text">{t('res.chat.dropHint')}</span>
      </div>

      {loadError ? (
            <div className="reservation-chat-page__error text-danger small mb-2" role="alert">
              {loadError}
            </div>
          ) : null}

          <div className="reservation-chat__history-nav" aria-label={t('res.chat.loadOlder')}>
            <div className="reservation-chat__messages-stage">
              {!loading && pageLinks.prev ? (
                <div className="reservation-chat__load-page-host reservation-chat__load-page-host--older">
                  <button
                    type="button"
                    className="reservation-chat__load-page reservation-chat__load-page--older btn btn-sm btn-light border shadow-sm"
                    disabled={paging}
                    onClick={() => void goToPage(pageLinks.prev)}
                  >
                    <i className="bi bi-chevron-up" aria-hidden="true" />
                    <span className="reservation-chat__load-page-label">{t('res.chat.loadOlder')}</span>
                  </button>
                </div>
              ) : null}

              <div
                ref={messagesRef}
                className={[
                  'reservation-chat__messages',
                  'reservation-chat-page__messages',
                  loading || showEmptyState ? 'reservation-chat-page__messages--empty' : '',
                ]
                  .filter(Boolean)
                  .join(' ')}
                role="log"
                aria-live="polite"
              >
                {showDayBar ? (
                  <div
                    className={`reservation-chat-day-bar${dayBarScrolling ? ' reservation-chat-day-bar--scrolling' : ''}`}
                    aria-live="polite"
                  >
                    <span className="reservation-chat__day-pill reservation-chat-day-bar__label">
                      {stickyDayKey ? formatDayLabel(stickyDayKey, t, i18n.language) : ''}
                    </span>
                  </div>
                ) : null}

                {loading ? (
                  <p className="text-muted small mb-0 reservation-chat__empty">{t('res.detail.loading')}</p>
                ) : null}

                {showEmptyState ? (
                  <p className="text-muted small mb-0 reservation-chat__empty">{t('res.chat.empty')}</p>
                ) : null}

                {dayGroups.map((group) => (
                  <Fragment key={group.dayKey || 'sin-fecha'}>
                    <div
                      className="reservation-chat__day-separator"
                      data-day-key={group.dayKey}
                      ref={(node) => registerSeparator(group.dayKey, node)}
                    >
                      <span className="reservation-chat__day-pill reservation-chat__day-separator-pill">
                        {formatDayLabel(group.dayKey, t, i18n.language)}
                      </span>
                    </div>
                    <div className="reservation-chat__day-group" data-day-key={group.dayKey}>
                      {group.messages.map((msg) => {
                        const senderId = idFromUri(msg.links.sender);
                        const mine = myId != null && senderId === myId;
                        return (
                          <ChatMessageBubble key={msg.links.self} msg={msg} mine={mine} locale={i18n.language} />
                        );
                      })}
                    </div>
                  </Fragment>
                ))}

              </div>

              {!loading && pageLinks.next ? (
                <div className="reservation-chat__load-page-host reservation-chat__load-page-host--newer">
                  <button
                    type="button"
                    className="reservation-chat__load-page reservation-chat__load-page--newer btn btn-sm btn-light border shadow-sm"
                    disabled={paging}
                    onClick={() => void goToPage(pageLinks.next)}
                  >
                    <i className="bi bi-chevron-down" aria-hidden="true" />
                    <span className="reservation-chat__load-page-label">{t('res.chat.loadNewer')}</span>
                  </button>
                </div>
              ) : null}
            </div>
          </div>

          {file ? (
            <div className="reservation-chat__pending" aria-live="polite">
              <span className="reservation-chat__pending-name">
                {file.name} ({formatFileSize(file.size)})
              </span>
              <button type="button" className="btn btn-sm btn-outline-secondary" onClick={clearPendingFile}>
                {t('res.chat.cancel')}
              </button>
            </div>
          ) : null}

          {uploadProgress != null ? (
            <div
              className="reservation-chat__upload-progress"
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={Math.round(uploadProgress)}
            >
              <div
                className="reservation-chat__upload-progress-bar"
                style={{ width: `${Math.min(100, Math.max(0, uploadProgress))}%` }}
              />
            </div>
          ) : null}

          <form
            onSubmit={(e) => void onSend(e)}
            className="reservation-chat__composer reservation-chat-page__composer d-flex gap-2 align-items-end mt-3"
          >
            <input
              ref={fileInputRef}
              type="file"
              className="visually-hidden"
              accept={CHAT_FILE_ACCEPT}
              onChange={(e) => setPendingFile(e.target.files?.[0] ?? null)}
            />
            <button
              type="button"
              className="btn btn-outline-secondary flex-shrink-0"
              title={t('res.chat.attach')}
              aria-label={t('res.chat.attach')}
              disabled={composerDisabled}
              onClick={() => fileInputRef.current?.click()}
            >
              <i className="bi bi-paperclip" aria-hidden="true" />
            </button>
            <label className="visually-hidden" htmlFor="chatBody">
              {t('res.chat.message')}
            </label>
            <textarea
              id="chatBody"
              className="form-control reservation-chat-page__input"
              rows={1}
              maxLength={getChatMessageMaxLength()}
              value={body}
              disabled={composerDisabled}
              onChange={(e) => setBody(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  void onSend();
                }
              }}
              placeholder={t('res.chat.placeholder')}
            />
            <button type="submit" className="btn btn-primary flex-shrink-0" disabled={composerDisabled}>
              {sending || uploading ? t('res.chat.sending') : t('res.chat.send')}
            </button>
          </form>

      {sendError ? (
        <div className="reservation-chat-page__error text-danger small mt-2" role="alert">
          {sendError}
        </div>
      ) : null}
    </div>
  );

  return (
    <div
      className="reservation-chat-widget"
      role="complementary"
      aria-label={t('res.chat.title')}
      aria-expanded={expanded}
    >
      {!expanded ? (
        <button
          type="button"
          className="reservation-chat-widget__launcher"
          onClick={() => onExpandedChange(true)}
          aria-expanded="false"
        >
          <ChatAvatar
            avatarUrl={counterparty?.avatarUrl}
            forename={counterparty?.forename}
            surname={counterparty?.surname}
            className="reservation-chat-widget__avatar rounded-circle flex-shrink-0"
          />
          <span className="reservation-chat-widget__launcher-name text-truncate">{displayName}</span>
          <i className="bi bi-chevron-up reservation-chat-widget__launcher-icon" aria-hidden="true" />
        </button>
      ) : (
        <div className="reservation-chat-widget__window">
          <header className="reservation-chat-widget__header">
            <ChatAvatar
              avatarUrl={counterparty?.avatarUrl}
              forename={counterparty?.forename}
              surname={counterparty?.surname}
              className="reservation-chat-widget__avatar rounded-circle flex-shrink-0"
            />
            <span className="reservation-chat-widget__header-name text-truncate">{displayName}</span>
            <button
              type="button"
              className="reservation-chat-widget__minimize"
              onClick={() => onExpandedChange(false)}
              aria-label={t('res.chat.minimize')}
            >
              <i className="bi bi-dash-lg" aria-hidden="true" />
            </button>
          </header>
          <section className="reservation-chat-card reservation-chat-card--floating bg-white">
            <div className="reservation-chat-card__body">{chatShell}</div>
          </section>
        </div>
      )}
    </div>
  );
}
