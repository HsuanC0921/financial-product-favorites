package io.github.hsuanc0921.favorites.presentation;

import io.github.hsuanc0921.favorites.service.UserService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 登入者自己的資料與扣款帳號 */
@RestController
class MeController {

	private final UserService userService;

	MeController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/api/me")
	MeResponse me(Principal principal) {
		String userId = principal.getName();
		return MeResponse.of(userService.profile(userId), userService.accounts(userId));
	}
}
