package io.github.hsuanc0921.favorites.common.model;

import java.math.BigDecimal;

/** 新增或修改時由使用者提供的欄位，金額由服務層算出 */
public record FavoriteInput(
	String productName,
	BigDecimal price,
	BigDecimal feeRate,
	int purchaseQuantity,
	String account,
	String note
) {}
