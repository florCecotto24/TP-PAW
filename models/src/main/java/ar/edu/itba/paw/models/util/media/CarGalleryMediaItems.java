package ar.edu.itba.paw.models.util.media;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.dto.car.CarGalleryMediaItem;
import ar.edu.itba.paw.models.dto.car.CarGalleryMediaItem.MediaKind;

/** Builds {@link CarGalleryMediaItem} lists from ordered {@link CarPicture} rows. */
public final class CarGalleryMediaItems {

    private CarGalleryMediaItems() {
    }

    public static List<CarGalleryMediaItem> fromPictures(final List<CarPicture> pictures) {
        if (pictures == null || pictures.isEmpty()) {
            return List.of();
        }
        final List<CarPicture> ordered = new ArrayList<>(pictures);
        ordered.sort(Comparator.comparingInt(CarPicture::getDisplayOrder));
        final List<CarGalleryMediaItem> items = new ArrayList<>(ordered.size());
        for (final CarPicture picture : ordered) {
            if (picture.isVideo()) {
                final StoredFile video = picture.getStoredFile();
                if (video != null) {
                    items.add(new CarGalleryMediaItem(
                            MediaKind.VIDEO,
                            "/car-media/" + video.getId(),
                            video.getContentType()));
                }
            } else {
                final Image image = picture.getImage();
                if (image != null) {
                    items.add(new CarGalleryMediaItem(
                            MediaKind.IMAGE, "/image/" + image.getId(), image.getContentType()));
                }
            }
        }
        return List.copyOf(items);
    }
}
