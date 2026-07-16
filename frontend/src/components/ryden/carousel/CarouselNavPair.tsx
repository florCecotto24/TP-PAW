/** Prev/next pair with fixed-width slots so hiding one arrow does not shift the other. */
export interface CarouselNavPairProps {
  showPrev: boolean;
  showNext: boolean;
  onPrev?: () => void;
  onNext?: () => void;
  prevLabel: string;
  nextLabel: string;
  className?: string;
}

function NavSlot({
  show,
  label,
  onClick,
  direction,
}: {
  show: boolean;
  label: string;
  onClick?: () => void;
  direction: 'prev' | 'next';
}) {
  const icon = direction === 'prev' ? 'bi-chevron-left' : 'bi-chevron-right';
  return (
    <span className="carouselHeader__nav-slot">
      {show ? (
        <button
          className="btn btn-sm btn-outline-secondary"
          type="button"
          onClick={onClick}
          aria-label={label}
        >
          <i className={`bi ${icon}`} aria-hidden="true"></i>
        </button>
      ) : (
        <span className="carouselHeader__nav-slot-placeholder" aria-hidden="true" />
      )}
    </span>
  );
}

export default function CarouselNavPair({
  showPrev,
  showNext,
  onPrev,
  onNext,
  prevLabel,
  nextLabel,
  className = '',
}: CarouselNavPairProps) {
  if (!showPrev && !showNext) {
    return null;
  }
  return (
    <div className={`carouselHeader__nav d-flex gap-2 align-items-center ${className}`.trim()}>
      <NavSlot show={showPrev} label={prevLabel} onClick={onPrev} direction="prev" />
      <NavSlot show={showNext} label={nextLabel} onClick={onNext} direction="next" />
    </div>
  );
}
