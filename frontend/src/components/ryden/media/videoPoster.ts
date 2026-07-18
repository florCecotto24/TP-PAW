/** Blob/remote URLs often render a blank first frame; `#t=0.001` nudges browsers to paint a poster. */
export function videoPreviewSrc(url: string): string {
  return url.includes('#') ? url : `${url}#t=0.001`;
}

/** Seek slightly past 0 so Safari/Chrome paint a thumbnail with `preload="metadata"`. */
export function paintVideoPoster(video: HTMLVideoElement): void {
  if (video.currentTime > 0) return;
  try {
    video.currentTime = 0.001;
  } catch {
    // Ignore seek errors (still loading / codec unsupported).
  }
}
