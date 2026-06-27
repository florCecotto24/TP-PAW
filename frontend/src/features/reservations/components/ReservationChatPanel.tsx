import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { idFromUri, listMessages, openBinaryLink, sendMessage } from '../api';
import { mergeMessages, latestMessageId } from '../chatLog';
import { useCurrentUser } from '../useCurrentUser';
import type { MessageDto, ReservationDto } from '../types';

export default function ReservationChatPanel({ reservation }: { reservation: ReservationDto }) {
  const { t } = useTranslation();
  const { id: myId } = useCurrentUser();
  const messagesUri = reservation.links.messages;
  const [messages, setMessages] = useState<MessageDto[]>([]);
  const [body, setBody] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!messagesUri) return;
    let cancelled = false;

    const load = async (afterId?: number) => {
      try {
        const res = await listMessages(messagesUri, afterId ? { afterId } : { page: 1 });
        const incoming = res.data ?? [];
        if (cancelled) return;
        setMessages((prev) => (afterId ? mergeMessages(prev, incoming) : incoming));
        setError(null);
      } catch {
        if (!cancelled) setError(t('res.chat.error'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    void load();
    const timer = window.setInterval(() => {
      setMessages((prev) => {
        const after = latestMessageId(prev);
        if (after > 0) void load(after);
        return prev;
      });
    }, 5000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [messagesUri, t]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  const onSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!messagesUri || sending) return;
    if (!body.trim() && !file) return;
    setSending(true);
    setError(null);
    try {
      const res = await sendMessage(messagesUri, body, file);
      if (res.data) {
        setMessages((prev) => mergeMessages(prev, [res.data as MessageDto]));
      }
      setBody('');
      setFile(null);
    } catch {
      setError(t('res.chat.error'));
    } finally {
      setSending(false);
    }
  };

  return (
    <section className="bg-white rounded-4 shadow-sm p-4 mt-4">
      <h2 className="h5 fw-semibold mb-3">{t('res.chat.title')}</h2>
      {loading ? <p className="text-secondary small">{t('res.detail.loading')}</p> : null}
      {error ? <div className="alert alert-danger py-2">{error}</div> : null}

      <div className="border rounded-3 p-3 mb-3 bg-light" style={{ maxHeight: 320, overflowY: 'auto' }}>
        {messages.length === 0 && !loading ? (
          <p className="text-secondary small mb-0">{t('res.chat.empty')}</p>
        ) : null}
        {messages.map((msg) => {
          const senderId = idFromUri(msg.links.sender);
          const mine = myId != null && senderId === myId;
          return (
            <div
              key={msg.links.self}
              className={`mb-2 d-flex ${mine ? 'justify-content-end' : 'justify-content-start'}`}
            >
              <div className={`rounded-3 px-3 py-2 small ${mine ? 'bg-primary text-white' : 'bg-white border'}`}>
                <p className="mb-1">{msg.body || '—'}</p>
                {msg.hasAttachment && msg.links.attachment ? (
                  <button
                    type="button"
                    className={`btn btn-link btn-sm p-0 ${mine ? 'text-white' : ''}`}
                    onClick={() => void openBinaryLink(msg.links.attachment as string)}
                  >
                    {t('res.chat.attachment')}
                  </button>
                ) : null}
                <div className={`text-end opacity-75 ${mine ? 'text-white' : 'text-secondary'}`} style={{ fontSize: '0.75rem' }}>
                  {new Date(msg.createdAt).toLocaleString()}
                </div>
              </div>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      <form onSubmit={onSend} className="row g-2 align-items-end">
        <div className="col-12">
          <label className="form-label small mb-1" htmlFor="chatBody">{t('res.chat.message')}</label>
          <textarea
            id="chatBody"
            className="form-control form-control-sm"
            rows={2}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder={t('res.chat.placeholder')}
          />
        </div>
        <div className="col-md-6">
          <label className="form-label small mb-1" htmlFor="chatFile">{t('res.chat.attach')}</label>
          <input
            id="chatFile"
            type="file"
            className="form-control form-control-sm"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
          {file ? <p className="small text-secondary mb-0 mt-1">{t('res.chat.attachSelected', { name: file.name })}</p> : null}
        </div>
        <div className="col-md-6 text-md-end">
          <button type="submit" className="btn btn-primary btn-sm" disabled={sending}>
            {sending ? t('res.chat.sending') : t('res.chat.send')}
          </button>
        </div>
      </form>
    </section>
  );
}
