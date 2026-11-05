package io.github.hsuanc0921.favorites.presentation;

import io.github.hsuanc0921.favorites.common.model.UserAccount;
import io.github.hsuanc0921.favorites.common.model.UserProfile;
import java.util.List;

/** GET /api/me 的回應 */
public record MeResponse(
	String userId,
	String userName,
	String email,
	String defaultAccount,
	List<AccountItem> accounts
) {
	public record AccountItem(String account, String accountName) {}

	static MeResponse of(UserProfile profile, List<UserAccount> accounts) {
		return new MeResponse(
			profile.userId(),
			profile.userName(),
			profile.email(),
			profile.account(),
			accounts
				.stream()
				.map(a -> new AccountItem(a.account(), a.accountName()))
				.toList()
		);
	}
}
