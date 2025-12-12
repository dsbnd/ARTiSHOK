package artishok.services;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

@Service
public class TokenBlacklistService {

	private final Set<String> blacklistedTokens = Collections.synchronizedSet(new HashSet<>());

	public void blacklistToken(String token) {
		blacklistedTokens.add(token);

		System.out.println("Token blacklisted: " + token.substring(0, Math.min(20, token.length())) + "...");
	}

	public boolean isTokenBlacklisted(String token) {
		if (token == null || token.isEmpty()) {
			return false;
		}

		return blacklistedTokens.contains(token);
	}

	public void removeFromBlacklist(String token) {
		blacklistedTokens.remove(token);
	}

	public int getBlacklistSize() {
		return blacklistedTokens.size();

	}

	public void clearBlacklist() {
		blacklistedTokens.clear();

	}

	public boolean isTokenValid(String token) {
		return !isTokenBlacklisted(token);
	}

	@Scheduled(fixedRate = 3600000)
	public void cleanupExpiredTokens() {

		System.out.println("Blacklist cleanup executed. Current size: " + getBlacklistSize());
	}

	public boolean anyTokenBlacklisted(String... tokens) {
		for (String token : tokens) {
			if (isTokenBlacklisted(token)) {
				return true;
			}
		}
		return false;
	}
}