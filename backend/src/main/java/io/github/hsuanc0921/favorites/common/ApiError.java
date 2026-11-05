package io.github.hsuanc0921.favorites.common;

import org.springframework.http.HttpStatus;

/** 所有錯誤回應的固定格式 */
public record ApiError(String code, String message) {
	public static final ApiError LOGIN_FAILED = new ApiError("LOGIN_FAILED", "使用者 ID 或密碼錯誤");
	public static final ApiError UNAUTHENTICATED = new ApiError("UNAUTHENTICATED", "請先登入");
	public static final ApiError FORBIDDEN = new ApiError("FORBIDDEN", "這項操作被拒絕，請重新整理頁面後再試");

	public static final ApiError VERSION_CONFLICT = new ApiError(
		"VERSION_CONFLICT",
		"這筆資料剛被修改過，請重新載入後再操作"
	);
	public static final ApiError PRECONDITION_REQUIRED = new ApiError(
		"PRECONDITION_REQUIRED",
		"缺少 If-Match 標頭，無法確認你看到的是不是最新版本"
	);
	public static final ApiError UNKNOWN_ACCOUNT = new ApiError("UNKNOWN_ACCOUNT", "這個扣款帳號不在你的帳號清單裡");
	public static final ApiError ACCOUNT_TAKEN = new ApiError("ACCOUNT_TAKEN", "這個帳號已經登錄過了");

	/** 只用於寫死的常數，不含引號與反斜線 */
	public String toJson() {
		return "{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}";
	}

	public static ApiError of(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> new ApiError("INVALID_REQUEST", "請求內容不正確");
			case NOT_FOUND -> new ApiError("NOT_FOUND", "找不到這筆資料");
			case METHOD_NOT_ALLOWED -> new ApiError("METHOD_NOT_ALLOWED", "不支援這個操作");
			case UNSUPPORTED_MEDIA_TYPE -> new ApiError("UNSUPPORTED_MEDIA_TYPE", "不支援這種內容格式");
			case NOT_ACCEPTABLE -> new ApiError("NOT_ACCEPTABLE", "無法提供要求的回應格式");
			default -> status.is5xxServerError()
				? new ApiError("INTERNAL_ERROR", "系統發生錯誤，請稍後再試")
				: new ApiError("REQUEST_FAILED", "無法完成這個請求");
		};
	}
}
