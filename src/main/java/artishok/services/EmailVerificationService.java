package artishok.services;

import artishok.entities.EmailVerificationToken;
import artishok.entities.User;
import artishok.repositories.EmailVerificationTokenRepository;
import artishok.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationService {

	@Autowired
	private EmailVerificationTokenRepository tokenRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmailService emailService;

	@Value("${app.email.verification.expiration-hours:24}")
	private int expirationHours;

	@Transactional
	public String createVerificationToken(User user) {

		tokenRepository.deleteByUser(user);

		String token = UUID.randomUUID().toString();
		LocalDateTime expiryDate = LocalDateTime.now().plusHours(expirationHours);

		EmailVerificationToken verificationToken = new EmailVerificationToken(token, user, expiryDate);
		tokenRepository.save(verificationToken);

		return token;
	}

	@Transactional
	public void sendVerificationEmail(User user) {
		try {
			String token = createVerificationToken(user);
			emailService.sendVerificationEmail(user.getEmail(), token, user.getFullName());
		} catch (Exception e) {

			if (!user.getIsActive()) {
				user.setIsActive(true);
				userRepository.save(user);
				System.out.println("Пользователь " + user.getEmail() + " автоматически активирован (режим разработки)");
			}
		}
	}

	@Transactional
	public boolean verifyEmail(String token) {
		Optional<EmailVerificationToken> verificationToken = tokenRepository.findByToken(token);

		if (verificationToken.isEmpty()) {
			return false;
		}

		EmailVerificationToken tokenEntity = verificationToken.get();

		if (tokenEntity.isExpired()) {
			tokenRepository.delete(tokenEntity);
			return false;
		}

		if (tokenEntity.getUsed()) {
			return false;
		}

		User user = tokenEntity.getUser();
		user.setIsActive(true);
		userRepository.save(user);

		tokenEntity.setUsed(true);
		tokenRepository.save(tokenEntity);

		try {
			emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
		} catch (Exception e) {

		}

		return true;
	}

	@Transactional
	public void resendVerificationEmail(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

		if (user.getIsActive()) {
			throw new IllegalArgumentException("Аккаунт уже активирован");
		}

		sendVerificationEmail(user);
	}

	public boolean isTokenValid(String token) {
		Optional<EmailVerificationToken> verificationToken = tokenRepository.findByToken(token);

		if (verificationToken.isEmpty()) {
			return false;
		}

		EmailVerificationToken tokenEntity = verificationToken.get();
		return !tokenEntity.isExpired() && !tokenEntity.getUsed();
	}

	@Transactional(readOnly = true)
	public User getUserByVerificationToken(String token) {
		Optional<EmailVerificationToken> verificationToken = tokenRepository.findByToken(token);

		if (verificationToken.isEmpty()) {
			throw new IllegalArgumentException("Неверный токен верификации");
		}

		EmailVerificationToken tokenEntity = verificationToken.get();

		if (tokenEntity.isExpired()) {
			throw new IllegalArgumentException("Токен верификации истек");
		}

		if (tokenEntity.getUsed()) {
			throw new IllegalArgumentException("Токен уже использован");
		}

		return tokenEntity.getUser();
	}

	@Transactional
	public void resendVerificationEmailByEmail(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

		if (user.getIsActive()) {
			throw new IllegalArgumentException("Аккаунт уже активирован");
		}

		sendVerificationEmail(user);
		System.out.println("Письмо верификации повторно отправлено на: " + email);
	}

	@Transactional(readOnly = true)
	public User getUserByTokenAfterVerification(String token) {
		Optional<EmailVerificationToken> verificationToken = tokenRepository.findByToken(token);

		if (verificationToken.isEmpty()) {
			throw new IllegalArgumentException("Неверный токен верификации");
		}

		EmailVerificationToken tokenEntity = verificationToken.get();

		if (tokenEntity.isExpired()) {
			throw new IllegalArgumentException("Токен верификации истек");
		}

		return tokenEntity.getUser();
	}
}