package artishok.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import artishok.entities.ExhibitionHallMap;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExhibitionHallMapRepository extends JpaRepository<ExhibitionHallMap, Long> {

    @Query("SELECT DISTINCT m FROM ExhibitionHallMap m LEFT JOIN FETCH m.exhibitionStands WHERE m.id = :id")
    Optional<ExhibitionHallMap> findByIdWithStands(@Param("id") Long id);

    @Query("SELECT m FROM ExhibitionHallMap m WHERE m.exhibitionEvent.id = :eventId")
    List<ExhibitionHallMap> findByExhibitionEventId(@Param("eventId") Long eventId);

    @Query("SELECT DISTINCT m FROM ExhibitionHallMap m LEFT JOIN FETCH m.exhibitionStands WHERE m.exhibitionEvent.id = :eventId")
    List<ExhibitionHallMap> findByExhibitionEventIdWithStands(@Param("eventId") Long eventId);
}