package io.github.hsuanc0921.favorites.service;

import io.github.hsuanc0921.favorites.common.model.AuthUser;
import io.github.hsuanc0921.favorites.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** 把資料庫裡的使用者交給 Spring Security 驗證登入 */
@Service
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public AppUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		AuthUser user = userRepository
			.findForLogin(username)
			.orElseThrow(() -> new UsernameNotFoundException("user not found"));
		// 資料庫比對不分大小寫，所以登入後的身分一律用資料庫裡的 User ID
		return User.withUsername(user.userId()).password(user.passwordHash()).roles("USER").build();
	}
}
