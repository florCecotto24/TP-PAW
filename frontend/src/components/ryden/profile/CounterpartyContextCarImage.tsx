export interface CounterpartyContextCarImageProps {
  imageUrl?: string | null;
  altText?: string;
}

/** Espejo de {@code ryden:counterpartyContextCarImage}. */
export default function CounterpartyContextCarImage({
  imageUrl,
  altText = '',
}: CounterpartyContextCarImageProps) {
  return (
    <div className="counterparty-context-image">
      {imageUrl ? (
        <img src={imageUrl} alt={altText} className="counterparty-context-image__img" />
      ) : (
        <div className="counterparty-context-image__placeholder">
          <i className="bi bi-car-front" aria-hidden="true"></i>
        </div>
      )}
    </div>
  );
}
