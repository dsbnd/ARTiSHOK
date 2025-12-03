package artishok.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

	UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			EmailVerificationService emailVerificationService) {
		this.passwordEncoder = passwordEncoder;
		this.userRepository = userRepository;
		this.emailVerificationService = emailVerificationService;
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

		// Хеширование пароля
		user.setPasswordHash(passwordEncoder.encode(plainPassword));

		// Установка даты регистрации, если не установлена
		if (user.getRegistrationDate() == null) {
			user.setRegistrationDate(LocalDateTime.now());
		}

		// Если верификация email включена, аккаунт не активен до подтверждения
        if (emailVerificationEnabled) {
            user.setIsActive(false);
        } else {
            user.setIsActive(true);
        }
        
        User savedUser = userRepository.save(user);
        
        // Отправляем email для верификации, если включено
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

	// Обновление пользователя
	@Transactional
	public User updateUser(Long id, User updatedUser) {
		return userRepository.findById(id).map(existingUser -> {
			// Обновляем только разрешенные поля
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

			// Роль и email обычно не изменяются через этот метод
			// Для их изменения нужны отдельные методы с проверкой прав

			return userRepository.save(existingUser);
		}).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
	}

	// Удаление пользователя (мягкое удаление - деактивация)
	@Transactional
	public void deactivateUser(Long id) {
		userRepository.findById(id).ifPresent(user -> {
			user.setIsActive(false);
			userRepository.save(user);
		});
	}

	// Активация пользователя
	@Transactional
	public void activateUser(Long id) {
		userRepository.findById(id).ifPresent(user -> {
			user.setIsActive(true);
			userRepository.save(user);
		});
	}

	// Получение пользователей по роли
	public List<User> getUsersByRole(UserRole role) {
		return userRepository.findByRole(role);
	}

	// Получение активных пользователей
	public List<User> getActiveUsers() {
		return userRepository.findByIsActiveTrue();
	}

	// Поиск пользователей по имени
	public List<User> searchUsersByName(String name) {
		return userRepository.findByFullNameContainingIgnoreCase(name);
	}

	// Подсчет пользователей по роли
	public long countUsersByRole(UserRole role) {
		return userRepository.countByRole(role);
	}

	// Получение последних зарегистрированных пользователей
	public List<User> getRecentlyRegisteredUsers() {
		return userRepository.findTop10ByOrderByRegistrationDateDesc();
	}

	// Смена пароля
	@Transactional
	public void changePassword(Long userId, String newPassword) {
		userRepository.findById(userId).ifPresent(user -> {
			user.setPasswordHash(passwordEncoder.encode(newPassword));
			userRepository.save(user);
		});
	}

	// Проверка существования пользователя
	public boolean userExists(Long id) {
		return userRepository.existsById(id);
	}

	// Проверка существования email
	public boolean emailExists(String email) {
		return userRepository.existsByEmail(email);
	}

}
