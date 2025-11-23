package artishok.services;

import java.util.List;

import org.springframework.stereotype.Service;


import artishok.entities.User;
import artishok.repositories.UserRepository;


@Service
public class UserService {
	private final UserRepository userRepository;
	
	UserService(UserRepository userRepository){
		
		this.userRepository=userRepository;
	}
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}
}
