/** Maps a 0–5 rating to full + half star counts (same thresholds as legacy counterparty JSP). */
export function starsFromRating(rating: number): { fullStars: number; halfStar: boolean } {
  const fullStars = Math.floor(rating);
  const frac = rating - fullStars;
  const halfStar = frac >= 0.4 && frac <= 0.6;
  return { fullStars, halfStar };
}
