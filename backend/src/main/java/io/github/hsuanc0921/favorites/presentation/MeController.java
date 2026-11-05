package io.github.hsuanc0921.favorites.presentation;

import io.github.hsuanc0921.favorites.common.model.UserAccount;
import io.github.hsuanc0921.favorites.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@PostMapping("/api/me/accounts")
	ResponseEntity<MeResponse.AccountItem> addAccount(Principal principal, @Valid @RequestBody AccountRequest request) {
		UserAccount created = userService.addAccount(
			principal.getName(),
			request.account(),
			request.accountName().strip()
		);
		return ResponseEntity.created(URI.create("/api/me")).body(
			new MeResponse.AccountItem(created.account(), created.accountName())
		);
	}
}
