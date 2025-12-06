package artishok.repositories;

import artishok.entities.GalleryOwnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface GalleryOwnershipRepository extends JpaRepository<GalleryOwnership, Long> {

    List<GalleryOwnership> findByOwnerId(Long ownerId);
    List<GalleryOwnership> findByGalleryId(Long galleryId);
    Optional<GalleryOwnership> findByGalleryIdAndOwnerId(Long galleryId, Long ownerId);

    @Query("SELECT CASE WHEN COUNT(go) > 0 THEN true ELSE false END " +
            "FROM GalleryOwnership go " +
            "WHERE go.gallery.id = :galleryId AND go.owner.id = :ownerId")
    boolean existsByGalleryIdAndOwnerId(@Param("galleryId") Long galleryId,
                                        @Param("ownerId") Long ownerId);

    // Метод для установки основного владельца
    @Transactional
    @Modifying
    @Query("UPDATE GalleryOwnership go SET go.isPrimary = false WHERE go.gallery.id = :galleryId")
    void clearPrimaryOwners(@Param("galleryId") Long galleryId);

    // Метод для получения основного владельца
    @Query("SELECT go FROM GalleryOwnership go " +
            "WHERE go.gallery.id = :galleryId AND go.isPrimary = true")
    Optional<GalleryOwnership> findPrimaryOwner(@Param("galleryId") Long galleryId);
}