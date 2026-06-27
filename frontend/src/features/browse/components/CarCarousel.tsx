import { useMemo } from 'react';
import BrowseCarCard from './BrowseCarCard';
import type { CarDto } from '../types';

function chunk<T>(items: T[], size: number): T[][] {
  const out: T[][] = [];
  for (let i = 0; i < items.length; i += size) {
    out.push(items.slice(i, i + size));
  }
  return out;
}

/** Carrusel Bootstrap con 4 autos por slide (como carouselSection.tag del JSP). */
export default function CarCarousel({
  id,
  cars,
  title,
  subtitle,
}: {
  id: string;
  cars: CarDto[];
  title: string;
  subtitle: string;
}) {
  const slides = useMemo(() => chunk(cars, 4), [cars]);

  return (
    <>
      <div className="d-flex flex-wrap align-items-end justify-content-between gap-2 mb-3">
        <div>
          <h2 className="h4 fw-semibold mb-1">{title}</h2>
          <p className="text-secondary mb-0 small">{subtitle}</p>
        </div>
        {slides.length > 1 ? (
          <div className="d-flex gap-2">
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              data-bs-target={`#${id}`}
              data-bs-slide="prev"
              aria-label="Previous"
            >
              <i className="bi bi-chevron-left" aria-hidden="true"></i>
            </button>
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              data-bs-target={`#${id}`}
              data-bs-slide="next"
              aria-label="Next"
            >
              <i className="bi bi-chevron-right" aria-hidden="true"></i>
            </button>
          </div>
        ) : null}
      </div>

      <div id={id} className="carousel slide" data-bs-ride="false">
        <div className="carousel-inner">
          {slides.map((slideCars, index) => (
            <div key={index} className={`carousel-item${index === 0 ? ' active' : ''}`}>
              <div className="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-3 pb-3">
                {slideCars.map((car) => (
                  <div key={car.links.self} className="col d-flex justify-content-center">
                    <BrowseCarCard car={car} />
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
}
