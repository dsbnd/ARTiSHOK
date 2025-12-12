package artishok.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import artishok.entities.User;
import artishok.entities.enums.UserRole;
import artishok.repositories.UserRepository;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service

public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationService emailVerificationService;
	private final UserActivityLogService userActivityLogService;

	UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			EmailVerificationService emailVerificationService, UserActivityLogService userActivityLogService) {
		this.passwordEncoder = passwordEncoder;
		this.userRepository = userRepository;
		this.emailVerificationService = emailVerificationService;
		this.userActivityLogService = userActivityLogService;
	}

	@Value("${app.email.verification.enabled:true}")
	private boolean emailVerificationEnabled;

	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	@Transactional
	public User createUser(User user, String plainPassword) {
		if (userRepository.existsByEmail(user.getEmail())) {
			throw new IllegalArgumentException("Пользователь с таким email уже существует");
		}

		user.setPasswordHash(passwordEncoder.encode(plainPassword));

		if (user.getRegistrationDate() == null) {
			user.setRegistrationDate(LocalDateTime.now());
		}

		if (emailVerificationEnabled) {
			user.setIsActive(false);
		} else {
			user.setIsActive(true);
		}

		User savedUser = userRepository.save(user);

		if (emailVerificationEnabled) {
			emailVerificationService.sendVerificationEmail(savedUser);
		}

		return savedUser;
	}

	public Optional<User> getUserById(Long id) {
		return userRepository.findById(id);
	}

	public Optional<User> getUserByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	@Transactional
	public User updateUser(Long id, User updatedUser) {
		return userRepository.findById(id).map(existingUser -> {

			if (updatedUser.getFullName() != null) {
				existingUser.setFullName(updatedUser.getFullName());
			}
			if (updatedUser.getPhoneNumber() != null) {
				existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
			}
			if (updatedUser.getBio() != null) {
				existingUser.setBio(updatedUser.getBio());
			}
			if (updatedUser.getAvatarUrl() != null) {
				existingUser.setAvatarUrl(updatedUser.getAvatarUrl());
			}
			if (updatedUser.getIsActive() != null) {
				existingUser.setIsActive(updatedUser.getIsActive());
			}

			return userRepository.save(existingUser);
		}).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
	}

	@Transactional
	public void deactivateUser(Long id) {
		userRepository.findById(id).ifPresent(user -> {
			user.setIsActive(false);
			userRepository.save(user);
		});
	}

	@Transactional
	public void activateUser(Long id) {
		userRepository.findById(id).ifPresent(user -> {
			user.setIsActive(true);
			userRepository.save(user);
		});
	}

	public List<User> getUsersByRole(UserRole role) {
		return userRepository.findByRole(role);
	}

	public List<User> getActiveUsers() {
		return userRepository.findByIsActiveTrue();
	}

	public List<User> searchUsersByName(String name) {
		return userRepository.findByFullNameContainingIgnoreCase(name);
	}

	public long countUsersByRole(UserRole role) {
		return userRepository.countByRole(role);
	}

	public List<User> getRecentlyRegisteredUsers() {
		return userRepository.findTop10ByOrderByRegistrationDateDesc();
	}

	@Transactional
	public void changePassword(Long userId, String newPassword) {
		userRepository.findById(userId).ifPresent(user -> {
			user.setPasswordHash(passwordEncoder.encode(newPassword));
			userRepository.save(user);
		});
	}

	@Transactional
	public String resetUserPassword(Long userId) {

		User currentAdmin = getCurrentUser();
		if (currentAdmin.getRole() != UserRole.ADMIN) {
			throw new RuntimeException("Только администраторы могут сбрасывать пароли");
		}

		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

		if (user.getId().equals(currentAdmin.getId())) {
			throw new RuntimeException("Администратор не может сбросить свой собственный пароль");
		}

		String tempPassword = generateTemporaryPassword();

		user.setPasswordHash(passwordEncoder.encode(tempPassword));
		userRepository.save(user);

		if (userActivityLogService != null) {
			try {
				userActivityLogService.createLog(currentAdmin.getId(),
						String.format("RESET_PASSWORD_FOR_USER_%d", userId));
			} catch (Exception e) {

				System.err.println("Ошибка логирования сброса пароля: " + e.getMessage());
			}
		}

		return tempPassword;
	}

	private String generateTemporaryPassword() {
		String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
		String numbers = "0123456789";
		String allChars = upperCaseLetters + lowerCaseLetters + numbers;

		StringBuilder password = new StringBuilder();
		java.util.Random random = new java.util.Random();

		password.append(upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length())));
		password.append(lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length())));
		password.append(numbers.charAt(random.nextInt(numbers.length())));

		for (int i = 0; i < 5; i++) {
			password.append(allChars.charAt(random.nextInt(allChars.length())));
		}

		char[] passwordArray = password.toString().toCharArray();
		for (int i = passwordArray.length - 1; i > 0; i--) {
			int index = random.nextInt(i + 1);
			char temp = passwordArray[index];
			passwordArray[index] = passwordArray[i];
			passwordArray[i] = temp;
		}

		return new String(passwordArray);
	}

	public boolean userExists(Long id) {
		return userRepository.existsById(id);
	}

	public boolean emailExists(String email) {
		return userRepository.existsByEmail(email);
	}

	@Transactional
	public User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new RuntimeException("Пользователь не аутентифицирован");
		}

		Object principal = authentication.getPrincipal();

		if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {

			String email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
			return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
		} else if (principal instanceof String) {

			String email = (String) principal;
			return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
		} else if (principal instanceof User) {

			return (User) principal;
		} else {
			throw new RuntimeException("Неизвестный тип пользователя в SecurityContext");
		}
	}

	@Transactional
	public Optional<User> getCurrentUserOptional() {
		try {
			return Optional.of(getCurrentUser());
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	public boolean isCurrentUserAdmin() {
		try {
			User user = getCurrentUser();
			return user.getRole() == UserRole.ADMIN;
		} catch (RuntimeException e) {
			return false;
		}
	}

	public boolean isCurrentUserOwner(Long userId) {
		try {
			User user = getCurrentUser();
			return user.getId().equals(userId);
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Transactional
	public void setUserActive(Long userId, Boolean isActive) {
		userRepository.findById(userId).ifPresent(user -> {
			user.setIsActive(isActive);
			userRepository.save(user);
		});
	}

	@Transactional
	public void changeUserRole(Long userId, UserRole newRole) {
		userRepository.findById(userId).ifPresent(user -> {

			User currentUser = getCurrentUser();
			if (currentUser.getId().equals(userId)) {
				throw new RuntimeException("Нельзя изменить свою собственную роль");
			}

			user.setRole(newRole);
			userRepository.save(user);
		});
	}

	public long getTotalUsersCount() {
		return userRepository.count();
	}

	public long getActiveUsersCount() {
		return userRepository.countByIsActiveTrue();
	}

}
