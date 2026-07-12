import { useEffect, useState } from 'react';
import { avatarColorFromNames, initialsFromNames } from './avatarUtils';

export interface AvatarProps {
  src?: string | null;
  alt?: string;
  /** When set, initials and optional color derive from the user's name. */
  forename?: string | null;
  surname?: string | null;
  /** Explicit initials override (e.g. when names are not available). */
  initials?: string;
  className?: string;
  imgClassName?: string;
  placeholderClassName?: string;
  /** Colored circle behind initials (reviews, compact avatars). */
  colored?: boolean;
  /** Render the photo without the outer wrapper (review cards, chat bubbles). */
  barePhoto?: boolean;
  /** Person icon instead of initials when there is no photo. */
  iconFallback?: boolean;
}

/** Profile avatar with image fallback to initials or icon placeholder. */
export default function Avatar({
  src,
  alt = '',
  forename,
  surname,
  initials,
  className = 'counterparty-avatar',
  imgClassName = 'counterparty-avatar__img',
  placeholderClassName = 'counterparty-avatar__placeholder',
  colored = false,
  iconFallback = false,
  barePhoto = false,
}: AvatarProps) {
  const [failed, setFailed] = useState(false);
  useEffect(() => setFailed(false), [src]);

  const resolvedInitials = initials ?? initialsFromNames(forename, surname);
  const showPhoto = Boolean(src) && !failed;

  if (showPhoto) {
    if (barePhoto) {
      return (
        <img
          src={src!}
          alt={alt}
          className={imgClassName ?? className}
          onError={() => setFailed(true)}
        />
      );
    }
    return (
      <div className={className}>
        <img
          src={src!}
          alt={alt}
          className={imgClassName}
          onError={() => setFailed(true)}
        />
      </div>
    );
  }

  if (colored) {
    return (
      <div
        className={className}
        style={{ backgroundColor: avatarColorFromNames(forename, surname) }}
        aria-hidden={!alt}
        title={alt || undefined}
      >
        {resolvedInitials}
      </div>
    );
  }

  return (
    <div className={className}>
      <div className={placeholderClassName}>
        {iconFallback ? (
          <i className="bi bi-person-fill" aria-hidden="true" />
        ) : (
          <span>{resolvedInitials}</span>
        )}
      </div>
    </div>
  );
}
