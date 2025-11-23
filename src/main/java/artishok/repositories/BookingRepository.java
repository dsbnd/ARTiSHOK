package artishok.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import artishok.entities.Booking;



@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
	
}
