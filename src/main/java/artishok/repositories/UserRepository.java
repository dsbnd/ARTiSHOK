package artishok.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import artishok.entities.User;



@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	
}
