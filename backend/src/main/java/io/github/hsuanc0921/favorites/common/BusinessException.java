package io.github.hsuanc0921.favorites.common;

import org.springframework.http.HttpStatus;

/** 預期內的業務錯誤，帶著要回給前端的狀態碼與訊息 */
public class BusinessException extends RuntimeException {

	private final HttpStatus status;
	private final ApiError error;

	public BusinessException(HttpStatus status, ApiError error) {
		super(error.code());
		this.status = status;
		this.error = error;
	}

	public HttpStatus status() {
		return status;
	}

	public ApiError error() {
		return error;
	}
}
