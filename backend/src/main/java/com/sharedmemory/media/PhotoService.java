package com.sharedmemory.media;

import com.sharedmemory.common.exception.NotFoundException;
import com.sharedmemory.membership.EventMember;
import com.sharedmemory.membership.MembershipService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final MediaStorageService mediaStorageService;
    private final MembershipService membershipService;

    public PhotoService(
            PhotoRepository photoRepository,
            MediaStorageService mediaStorageService,
            MembershipService membershipService) {
        this.photoRepository = photoRepository;
        this.mediaStorageService = mediaStorageService;
        this.membershipService = membershipService;
    }

    @Transactional
    public Photo upload(UUID eventId, UUID userId, MultipartFile file) {
        EventMember member = membershipService.requireActiveMembership(eventId, userId);
        byte[] bytes = readBytes(file);
        BufferedImage image = decodeImage(bytes);

        StoredFile stored = mediaStorageService.store(bytes);
        Photo photo = Photo.builder()
                .eventId(eventId)
                .uploadedByMemberId(member.getId())
                .storageKey(stored.storageKey())
                .checksum(stored.checksum())
                .width(image.getWidth())
                .height(image.getHeight())
                .processingStatus(PhotoProcessingStatus.PENDING)
                .galleryVisible(false)
                .build();
        return photoRepository.save(photo);
    }

    @Transactional(readOnly = true)
    public Photo getById(UUID photoId) {
        return photoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("Photo not found: " + photoId));
    }

    @Transactional(readOnly = true)
    public BufferedImage getDecryptedImage(UUID photoId) {
        Photo photo = getById(photoId);
        return decodeImage(mediaStorageService.retrieve(photo.getStorageKey()));
    }

    @Transactional(readOnly = true)
    public List<Photo> getGallery(UUID eventId, UUID userId, Pageable pageable) {
        membershipService.requireActiveMembership(eventId, userId);
        return photoRepository.findByEventIdAndGalleryVisibleTrueOrderByCreatedAtDesc(eventId, pageable);
    }

    @Transactional
    public void updateProcessingStatus(UUID photoId, PhotoProcessingStatus status) {
        Photo photo = getById(photoId);
        photo.setProcessingStatus(status);
        photoRepository.save(photo);
    }

    @Transactional
    public void setGalleryVisible(UUID photoId, boolean visible) {
        Photo photo = getById(photoId);
        photo.setGalleryVisible(visible);
        photoRepository.save(photo);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded photo", e);
        }
    }

    private BufferedImage decodeImage(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IllegalArgumentException("Uploaded file is not a readable image");
            }
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to decode photo", e);
        }
    }
}
