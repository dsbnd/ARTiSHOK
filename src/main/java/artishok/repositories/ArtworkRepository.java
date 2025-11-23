package artishok.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import artishok.entities.Artwork;



@Repository
public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
	
}
