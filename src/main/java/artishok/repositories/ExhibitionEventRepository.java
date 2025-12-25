package artishok.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import artishok.entities.ExhibitionEvent;

import java.util.List;

@Repository
public interface ExhibitionEventRepository extends JpaRepository<ExhibitionEvent, Long> {

    @Query("SELECT e FROM ExhibitionEvent e WHERE e.gallery.id = :galleryId")
    List<ExhibitionEvent> findByGalleryId(@Param("galleryId") Long galleryId);
}