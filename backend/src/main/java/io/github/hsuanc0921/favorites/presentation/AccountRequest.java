package io.github.hsuanc0921.favorites.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 新增扣款帳號的請求內容 */
public record AccountRequest(
	@NotBlank @Pattern(regexp = "[0-9]{1,32}") String account,
	@NotBlank @Size(max = 50) String accountName
) {}
