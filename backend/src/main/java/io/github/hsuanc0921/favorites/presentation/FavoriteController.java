package io.github.hsuanc0921.favorites.presentation;

import io.github.hsuanc0921.favorites.common.model.Favorite;
import io.github.hsuanc0921.favorites.service.FavoriteService;
import io.github.hsuanc0921.favorites.service.UserService;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 喜好紀錄的新增、查詢、修改、刪除，只處理登入者自己的資料，身分取自登入狀態，不接受前端指定
 * <ul>
 * <li>查詢時以 ETag 回傳版本號，修改與刪除用 If-Match 帶回</li>
 * <li>缺少 If-Match 回 428，版本已過期回 412，格式不對回 400</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/favorites")
class FavoriteController {

	private final FavoriteService favoriteService;
	private final UserService userService;

	FavoriteController(FavoriteService favoriteService, UserService userService) {
		this.favoriteService = favoriteService;
		this.userService = userService;
	}

	@GetMapping
	FavoriteListResponse list(Principal principal, @RequestParam(defaultValue = "0") @Min(0) long afterSn) {
		String userId = principal.getName();
		return FavoriteListResponse.of(userService.profile(userId), favoriteService.list(userId, afterSn));
	}

	@GetMapping("/{sn}")
	ResponseEntity<FavoriteResponse> get(Principal principal, @PathVariable @Min(1) long sn) {
		Favorite favorite = favoriteService.get(principal.getName(), sn);
		return ResponseEntity.ok().eTag(String.valueOf(favorite.version())).body(FavoriteResponse.from(favorite));
	}
}
