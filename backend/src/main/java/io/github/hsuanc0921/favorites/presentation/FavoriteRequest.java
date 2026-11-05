package io.github.hsuanc0921.favorites.presentation;

import io.github.hsuanc0921.favorites.common.model.FavoriteInput;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 只收使用者能決定的欄位，使用者、金額、建立時間一律由伺服器決定 */
public record FavoriteRequest(
	@NotBlank @Size(max = 100) String productName,
	@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
	@NotNull @DecimalMin("0") @DecimalMax("1") @Digits(integer = 1, fraction = 8) BigDecimal feeRate,
	@Min(1) @Max(1_000_000) int purchaseQuantity,
	@NotNull @Pattern(regexp = "[0-9]{1,32}") String account,
	@Size(max = 200) String note
) {
	FavoriteInput toInput() {
		return new FavoriteInput(
			productName.strip(),
			price,
			feeRate,
			purchaseQuantity,
			account,
			note == null ? "" : note
		);
	}
}
