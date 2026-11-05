package io.github.hsuanc0921.favorites.repository;

import io.github.hsuanc0921.favorites.common.model.AuthUser;
import io.github.hsuanc0921.favorites.common.model.UserAccount;
import io.github.hsuanc0921.favorites.common.model.UserProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** 使用者與扣款帳號的資料存取，一律透過預存程序 */
@Repository
public class UserRepository {

	private final JdbcClient jdbc;

	public UserRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<AuthUser> findForLogin(String userId) {
		return jdbc
			.sql("CALL sp_user_find_for_login(?)")
			.param(userId)
			.query((rs, rowNum) -> new AuthUser(rs.getString("user_id"), rs.getString("password_hash")))
			.optional();
	}

	public Optional<UserProfile> findProfile(String userId) {
		return jdbc
			.sql("CALL sp_user_profile_get(?)")
			.param(userId)
			.query((rs, rowNum) ->
				new UserProfile(
					rs.getString("user_id"),
					rs.getString("user_name"),
					rs.getString("email"),
					rs.getString("account")
				)
			)
			.optional();
	}

	public List<UserAccount> listAccounts(String userId) {
		return jdbc
			.sql("CALL sp_user_accounts_list(?)")
			.param(userId)
			.query((rs, rowNum) -> new UserAccount(rs.getString("account"), rs.getString("account_name")))
			.list();
	}
}
