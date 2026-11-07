package io.github.hsuanc0921.favorites.repository;

import io.github.hsuanc0921.favorites.common.ApiError;
import io.github.hsuanc0921.favorites.common.BusinessException;
import io.github.hsuanc0921.favorites.common.FavoriteNotFoundException;
import io.github.hsuanc0921.favorites.common.model.Favorite;
import io.github.hsuanc0921.favorites.common.model.FavoriteInput;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * 喜好紀錄的資料存取，一律透過預存程序（CALL），參數以問號綁定，不拼接 SQL
 * <ul>
 * <li>每支預存程序都帶登入者的 userId，查不到就等同不存在，所以改別人的 SN 只會得到 404</li>
 * <li>修改與刪除另外比對版本號，預存程序用 SIGNAL 回報「找不到」或「版本衝突」</li>
 * <li>外鍵檢查失敗代表扣款帳號不屬於這位使用者</li>
 * <li>以上都在這裡轉成對應的業務例外，其餘錯誤原樣往上拋</li>
 * </ul>
 */
@Repository
public class FavoriteRepository {

	/** MySQL 外鍵檢查失敗，這裡只會是 (user_id, account) 對不到使用者自己的帳號 */
	private static final int ER_NO_REFERENCED_ROW = 1452;

	private final JdbcClient jdbc;

	public FavoriteRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<Favorite> list(String userId, long afterSn) {
		return jdbc
			.sql("CALL sp_favorite_list(?, ?)")
			.param(userId)
			.param(afterSn)
			.query(FavoriteRepository::map)
			.list();
	}

	public Optional<Favorite> find(String userId, long sn) {
		return jdbc.sql("CALL sp_favorite_get(?, ?)").param(userId).param(sn).query(FavoriteRepository::map).optional();
	}

	/** 回傳新紀錄的 sn，版本一律從 0 開始 */
	public long create(String userId, FavoriteInput in, BigDecimal totalFee, BigDecimal totalAmount) {
		return run(() ->
			jdbc
				.sql("CALL sp_favorite_create(?, ?, ?, ?, ?, ?, ?, ?, ?)")
				.params(
					List.of(
						userId,
						in.productName(),
						in.price(),
						in.feeRate(),
						in.purchaseQuantity(),
						in.account(),
						totalFee,
						totalAmount,
						in.note()
					)
				)
				.query((rs, rowNum) -> rs.getLong("sn"))
				.single()
		);
	}

	public void update(
		String userId,
		long sn,
		long expectedVersion,
		FavoriteInput in,
		BigDecimal totalFee,
		BigDecimal totalAmount
	) {
		run(() ->
			jdbc
				.sql("CALL sp_favorite_update(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
				.params(
					List.of(
						userId,
						sn,
						expectedVersion,
						in.productName(),
						in.price(),
						in.feeRate(),
						in.purchaseQuantity(),
						in.account(),
						totalFee,
						totalAmount,
						in.note()
					)
				)
				.query((rs, rowNum) -> rs.getLong("version"))
				.single()
		);
	}

	public void delete(String userId, long sn, long expectedVersion) {
		run(() ->
			jdbc
				.sql("CALL sp_favorite_delete(?, ?, ?)")
				.params(List.of(userId, sn, expectedVersion))
				.update()
		);
	}

	private static <T> T run(Supplier<T> call) {
		try {
			return call.get();
		} catch (DataAccessException e) {
			throw translate(e);
		}
	}

	/** 把預存程序的 SIGNAL 與外鍵錯誤轉成對應的業務例外，其他錯誤原樣丟出 */
	private static RuntimeException translate(DataAccessException e) {
		if (e.getCause() instanceof SQLException sql) {
			if ("45000".equals(sql.getSQLState())) {
				if ("FAVORITE_VERSION_CONFLICT".equals(sql.getMessage())) {
					return new BusinessException(HttpStatus.PRECONDITION_FAILED, ApiError.VERSION_CONFLICT);
				}
				if ("FAVORITE_NOT_FOUND".equals(sql.getMessage())) {
					return new FavoriteNotFoundException();
				}
			}
			if (sql.getErrorCode() == ER_NO_REFERENCED_ROW) {
				return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, ApiError.UNKNOWN_ACCOUNT);
			}
		}
		return e;
	}

	private static Favorite map(ResultSet rs, int rowNum) throws SQLException {
		// created_at 以 UTC 保存，直接當 UTC 讀，不經過連線時區轉換
		return new Favorite(
			rs.getLong("sn"),
			rs.getString("product_name"),
			rs.getBigDecimal("price"),
			rs.getBigDecimal("fee_rate"),
			rs.getInt("purchase_quantity"),
			rs.getString("account"),
			rs.getBigDecimal("total_fee"),
			rs.getBigDecimal("total_amount"),
			rs.getString("note"),
			rs.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
			rs.getLong("version")
		);
	}
}
