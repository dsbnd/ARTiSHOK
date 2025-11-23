package artishok.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import artishok.entities.UserActivityLog;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivityLog, Long> {

}
