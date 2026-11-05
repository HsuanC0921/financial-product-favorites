package io.github.hsuanc0921.favorites.presentation;

import io.github.hsuanc0921.favorites.common.model.FavoritePage;
import io.github.hsuanc0921.favorites.common.model.UserProfile;
import java.util.List;

/** Email 只在使用者區塊出現一次，不在每筆重複 */
public record FavoriteListResponse(User user, List<FavoriteResponse> items, Long nextAfterSn) {
	public record User(String userId, String userName, String email) {}

	static FavoriteListResponse of(UserProfile profile, FavoritePage page) {
		return new FavoriteListResponse(
			new User(profile.userId(), profile.userName(), profile.email()),
			page.items().stream().map(FavoriteResponse::from).toList(),
			page.nextAfterSn()
		);
	}
}
