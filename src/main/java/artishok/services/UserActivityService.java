package artishok.services;

import java.util.List;

import org.springframework.stereotype.Service;

import artishok.entities.UserActivityLog;
import artishok.repositories.UserActivityRepository;

@Service
public class UserActivityService {
	private final UserActivityRepository userActivityRepository;

	UserActivityService(UserActivityRepository userActivityRepository) {
		this.userActivityRepository = userActivityRepository;
	}

	public List<UserActivityLog> getAllUserActivityLogs() {
		return userActivityRepository.findAll();
	}
}
