package io.github.hsuanc0921.favorites.service;

import io.github.hsuanc0921.favorites.common.model.UserAccount;
import io.github.hsuanc0921.favorites.common.model.UserProfile;
import io.github.hsuanc0921.favorites.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** 使用者資料與扣款帳號的業務邏輯 */
@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserProfile profile(String userId) {
		return userRepository
			.findProfile(userId)
			.orElseThrow(() -> new IllegalStateException("logged-in user missing: " + userId));
	}

	public List<UserAccount> accounts(String userId) {
		return userRepository.listAccounts(userId);
	}
}
