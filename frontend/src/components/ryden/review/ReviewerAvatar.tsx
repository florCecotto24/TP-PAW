import Avatar from '../profile/Avatar';

export interface ReviewerAvatarProps {
  forename: string;
  surname: string;
  avatarUrl?: string | null;
  className?: string;
}

/** Review author avatar with photo fallback to colored initials. */
export default function ReviewerAvatar({
  forename,
  surname,
  avatarUrl,
  className = 'reviewer-avatar flex-shrink-0',
}: ReviewerAvatarProps) {
  return (
    <Avatar
      src={avatarUrl}
      forename={forename}
      surname={surname}
      className={className}
      imgClassName={className}
      colored
      barePhoto
    />
  );
}
