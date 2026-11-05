package io.github.hsuanc0921.favorites.presentation;

import io.github.hsuanc0921.favorites.common.model.Favorite;
import java.time.Instant;

/** 價格、費率與金額一律用十進位字串，避免被當成浮點數 */
public record FavoriteResponse(
	long sn,
	String productName,
	String price,
	String feeRate,
	int purchaseQuantity,
	String account,
	String totalFee,
	String totalAmount,
	String note,
	Instant createdAt,
	long version
) {
	static FavoriteResponse from(Favorite f) {
		return new FavoriteResponse(
			f.sn(),
			f.productName(),
			f.price().toPlainString(),
			f.feeRate().stripTrailingZeros().toPlainString(),
			f.purchaseQuantity(),
			f.account(),
			f.totalFee().toPlainString(),
			f.totalAmount().toPlainString(),
			f.note(),
			f.createdAt(),
			f.version()
		);
	}
}
