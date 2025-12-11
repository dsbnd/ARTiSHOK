package artishok.repositories;

import artishok.entities.Gallery;
import artishok.entities.User;
import artishok.entities.enums.GalleryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GalleryRepository extends JpaRepository<Gallery, Long> {


    List<Gallery> findByStatus(GalleryStatus status);
    List<Gallery> findByNameContainingIgnoreCase(String name);
    boolean existsByName(String name);
    boolean existsByContactEmail(String email);

    // Методы для получения галерей владельца (через gallery_ownership)
    @Query("SELECT g FROM Gallery g WHERE g.id IN " +
            "(SELECT go.gallery.id FROM GalleryOwnership go WHERE go.owner.id = :ownerId)")
    List<Gallery> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT g FROM Gallery g WHERE g.id IN " +
            "(SELECT go.gallery.id FROM GalleryOwnership go WHERE go.owner.id = :ownerId) " +
            "AND g.status = :status")
    List<Gallery> findByOwnerIdAndStatus(@Param("ownerId") Long ownerId,
                                         @Param("status") GalleryStatus status);

    // Метод для получения основного владельца галереи
    @Query("SELECT go.owner FROM GalleryOwnership go " +
            "WHERE go.gallery.id = :galleryId AND go.isPrimary = true")
    Optional<User> findPrimaryOwnerByGalleryId(@Param("galleryId") Long galleryId);
}