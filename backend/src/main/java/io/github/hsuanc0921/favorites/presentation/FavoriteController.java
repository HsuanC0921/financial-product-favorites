package io.github.hsuanc0921.favorites.presentation;

import io.github.hsuanc0921.favorites.common.ApiError;
import io.github.hsuanc0921.favorites.common.BusinessException;
import io.github.hsuanc0921.favorites.common.model.Favorite;
import io.github.hsuanc0921.favorites.service.FavoriteService;
import io.github.hsuanc0921.favorites.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

	@PostMapping
	ResponseEntity<FavoriteResponse> create(Principal principal, @Valid @RequestBody FavoriteRequest request) {
		Favorite created = favoriteService.create(principal.getName(), request.toInput());
		return ResponseEntity.created(URI.create("/api/favorites/" + created.sn()))
			.eTag(String.valueOf(created.version()))
			.body(FavoriteResponse.from(created));
	}

	@PutMapping("/{sn}")
	ResponseEntity<FavoriteResponse> update(
		Principal principal,
		@PathVariable @Min(1) long sn,
		@RequestHeader(value = "If-Match", required = false) String ifMatch,
		@Valid @RequestBody FavoriteRequest request
	) {
		Favorite updated = favoriteService.update(principal.getName(), sn, expectedVersion(ifMatch), request.toInput());
		return ResponseEntity.ok().eTag(String.valueOf(updated.version())).body(FavoriteResponse.from(updated));
	}

	@DeleteMapping("/{sn}")
	ResponseEntity<Void> delete(
		Principal principal,
		@PathVariable @Min(1) long sn,
		@RequestHeader(value = "If-Match", required = false) String ifMatch
	) {
		favoriteService.delete(principal.getName(), sn, expectedVersion(ifMatch));
		return ResponseEntity.noContent().build();
	}

	/** If-Match 帶的是讀取時拿到的 ETag，例如 "3"，缺少就要求補上，格式不對回 400 */
	private static long expectedVersion(String ifMatch) {
		if (ifMatch == null) {
			throw new BusinessException(HttpStatus.PRECONDITION_REQUIRED, ApiError.PRECONDITION_REQUIRED);
		}
		String value = ifMatch.strip();
		if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
			value = value.substring(1, value.length() - 1);
		}
		try {
			long version = Long.parseLong(value);
			if (version >= 0) {
				return version;
			}
		} catch (NumberFormatException ignored) {
			// 落到下面統一回 400
		}
		throw new BusinessException(HttpStatus.BAD_REQUEST, ApiError.of(HttpStatus.BAD_REQUEST));
	}
}
