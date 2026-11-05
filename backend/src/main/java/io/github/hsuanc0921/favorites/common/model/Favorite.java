package io.github.hsuanc0921.favorites.common.model;

import java.math.BigDecimal;
import java.time.Instant;

/** 一筆喜好紀錄，產品欄位與金額一併帶出，金額由服務層計算後存入 */
public record Favorite(
	long sn,
	String productName,
	BigDecimal price,
	BigDecimal feeRate,
	int purchaseQuantity,
	String account,
	BigDecimal totalFee,
	BigDecimal totalAmount,
	String note,
	Instant createdAt,
	long version
) {}
