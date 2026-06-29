export interface SpecCardProps {
  icon: string;
  label: string;
}

/** Espejo de {@code ryden:specCard}. */
export default function SpecCard({ icon, label }: SpecCardProps) {
  return (
    <div className="spec-card border rounded-4 p-3 text-center h-100 d-flex flex-column align-items-center justify-content-center gap-2">
      <i className={`bi bi-${icon} fs-3 text-primary`} aria-hidden="true"></i>
      <span className="small fw-medium text-dark">{label}</span>
    </div>
  );
}
