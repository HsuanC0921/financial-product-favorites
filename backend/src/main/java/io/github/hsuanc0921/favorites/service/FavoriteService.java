package io.github.hsuanc0921.favorites.service;

import io.github.hsuanc0921.favorites.common.FavoriteNotFoundException;
import io.github.hsuanc0921.favorites.common.model.Favorite;
import io.github.hsuanc0921.favorites.common.model.FavoriteInput;
import io.github.hsuanc0921.favorites.common.model.FavoritePage;
import io.github.hsuanc0921.favorites.repository.FavoriteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 喜好紀錄的業務邏輯，資料存取交給 Repository
 * <ul>
 * <li>金額一律在這裡計算，不採用前端傳來的數字：手續費 = 價格 × 數量 × 費率，四捨五入到分，總金額 = 價格 × 數量 + 手續費</li>
 * <li>金額用 BigDecimal，避免浮點誤差</li>
 * <li>新增、修改、刪除各自是一個交易，預存程序不 COMMIT，成敗由 @Transactional 決定，
 * 所以 Product 與 Like List 任何一步失敗都整筆回復，不會留下沒有 Like List 的 Product</li>
 * <li>修改與刪除要帶讀取時的版本號，資料已被別處修改過就拒絕，不會蓋掉別人的修改</li>
 * </ul>
 */
@Service
public class FavoriteService {

	/** 與 sp_favorite_list 的 LIMIT 相同 */
	static final int PAGE_SIZE = 100;

	private final FavoriteRepository favoriteRepository;

	public FavoriteService(FavoriteRepository favoriteRepository) {
		this.favoriteRepository = favoriteRepository;
	}

	public FavoritePage list(String userId, long afterSn) {
		List<Favorite> items = favoriteRepository.list(userId, afterSn);
		Long nextAfterSn = items.size() == PAGE_SIZE ? items.get(items.size() - 1).sn() : null;
		return new FavoritePage(items, nextAfterSn);
	}

	public Favorite get(String userId, long sn) {
		return favoriteRepository.find(userId, sn).orElseThrow(FavoriteNotFoundException::new);
	}

	/** Product 與 Like List 兩張表要一起成功，任何一步失敗都整筆回復 */
	@Transactional
	public Favorite create(String userId, FavoriteInput in) {
		BigDecimal fee = fee(in);
		long sn = favoriteRepository.create(userId, in, fee, total(in, fee));
		// 在同一個交易裡讀回，回傳的是資料庫實際存的內容（含建立時間與版本）
		return get(userId, sn);
	}

	@Transactional
	public Favorite update(String userId, long sn, long expectedVersion, FavoriteInput in) {
		BigDecimal fee = fee(in);
		favoriteRepository.update(userId, sn, expectedVersion, in, fee, total(in, fee));
		return get(userId, sn);
	}

	@Transactional
	public void delete(String userId, long sn, long expectedVersion) {
		favoriteRepository.delete(userId, sn, expectedVersion);
	}

	/** 手續費 = 價格 × 數量 × 費率，四捨五入到分 */
	static BigDecimal fee(FavoriteInput in) {
		return principal(in).multiply(in.feeRate()).setScale(2, RoundingMode.HALF_UP);
	}

	/** 總金額 = 價格 × 數量 + 手續費 */
	static BigDecimal total(FavoriteInput in, BigDecimal fee) {
		return principal(in).add(fee);
	}

	private static BigDecimal principal(FavoriteInput in) {
		return in.price().multiply(BigDecimal.valueOf(in.purchaseQuantity()));
	}
}
