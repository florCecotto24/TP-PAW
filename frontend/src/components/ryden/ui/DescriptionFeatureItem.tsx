export interface DescriptionFeatureItemProps {
  title: string;
  subtitle: string;
}

/** Espejo de {@code ryden:descriptionFeatureItem}. */
export default function DescriptionFeatureItem({ title, subtitle }: DescriptionFeatureItemProps) {
  return (
    <div className="d-flex gap-3 descriptionFeatureItem">
      <i className="bi bi-check-circle-fill text-primary flex-shrink-0 mt-1" aria-hidden="true"></i>
      <div>
        <p className="fw-semibold mb-1 text-dark">{title}</p>
        <p className="text-secondary small mb-0">{subtitle}</p>
      </div>
    </div>
  );
}
