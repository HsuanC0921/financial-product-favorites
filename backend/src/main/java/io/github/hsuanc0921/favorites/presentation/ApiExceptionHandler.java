package io.github.hsuanc0921.favorites.presentation;

import io.github.hsuanc0921.favorites.common.ApiError;
import io.github.hsuanc0921.favorites.common.FavoriteNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** 所有錯誤都轉成固定的 {code, message}，不回傳 SQL 或堆疊 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(FavoriteNotFoundException.class)
	ResponseEntity<ApiError> notFound() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(HttpStatus.NOT_FOUND));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> unexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
			ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR)
		);
	}

	/** Spring MVC 內建的 400、404、405 等例外也統一格式 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
		Exception ex,
		Object body,
		HttpHeaders headers,
		HttpStatusCode statusCode,
		WebRequest request
	) {
		ApiError error = ApiError.of(HttpStatus.valueOf(statusCode.value()));
		return new ResponseEntity<>(error, headers, statusCode);
	}
}
