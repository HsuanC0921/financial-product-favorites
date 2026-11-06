package io.github.hsuanc0921.favorites;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * MockMvc 看不到真正的 Cookie 屬性和 Session 輪替，所以這裡改用真的 HTTP 連線
 * 同樣要先建好資料庫
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CookieAndSessionTests {

	@LocalServerPort
	private int port;

	private final HttpClient client = HttpClient.newHttpClient();

	private HttpResponse<String> send(String method, String path, String cookies, String csrf, String form)
		throws Exception {
		return send(method, path, cookies, csrf, form, false);
	}

	/** behindHttpsProxy：模擬 Nginx 終止 TLS 後轉送過來的請求 */
	private HttpResponse<String> send(
		String method,
		String path,
		String cookies,
		String csrf,
		String form,
		boolean behindHttpsProxy
	) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
		if (behindHttpsProxy) {
			builder.header("X-Forwarded-Proto", "https");
		}
		if (cookies != null) {
			builder.header("Cookie", cookies);
		}
		if (csrf != null) {
			builder.header("X-XSRF-TOKEN", csrf);
		}
		if (form != null) {
			builder.header("Content-Type", "application/x-www-form-urlencoded");
		}
		builder.method(
			method,
			form == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(form)
		);
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static Optional<String> setCookie(HttpResponse<?> response, String name) {
		List<String> headers = response.headers().allValues("Set-Cookie");
		return headers
			.stream()
			.filter(h -> h.startsWith(name + "="))
			.findFirst();
	}

	private static String value(String setCookieHeader) {
		return setCookieHeader.substring(setCookieHeader.indexOf('=') + 1, setCookieHeader.indexOf(';'));
	}

	private static String loginForm() {
		return (
			"userId=" +
			URLEncoder.encode("A1236456789", StandardCharsets.UTF_8) +
			"&password=" +
			URLEncoder.encode("DemoUserA#2026", StandardCharsets.UTF_8)
		);
	}

	@Test
	void sessionCookieIsHttpOnlyAndLoginRotatesTheSession() throws Exception {
		String csrf = value(setCookie(send("GET", "/api/me", null, null, null), "XSRF-TOKEN").orElseThrow());
		String csrfCookie = "XSRF-TOKEN=" + csrf;

		HttpResponse<String> first = send("POST", "/api/auth/login", csrfCookie, csrf, loginForm());
		assertThat(first.statusCode()).isEqualTo(204);
		String firstSession = setCookie(first, "JSESSIONID").orElseThrow();
		assertThat(firstSession).contains("HttpOnly").contains("SameSite=Lax");
		String firstId = value(firstSession);
		assertThat(send("GET", "/api/me", "JSESSIONID=" + firstId, null, null).statusCode()).isEqualTo(200);

		// 帶著已登入的 Session 再登入一次，要換成新的 Session ID，舊的失效
		HttpResponse<String> second = send(
			"POST",
			"/api/auth/login",
			csrfCookie + "; JSESSIONID=" + firstId,
			csrf,
			loginForm()
		);
		assertThat(second.statusCode()).isEqualTo(204);
		String secondId = value(setCookie(second, "JSESSIONID").orElseThrow());
		assertThat(secondId).isNotEqualTo(firstId);
		assertThat(send("GET", "/api/me", "JSESSIONID=" + firstId, null, null).statusCode()).isEqualTo(401);
		assertThat(send("GET", "/api/me", "JSESSIONID=" + secondId, null, null).statusCode()).isEqualTo(200);
	}

	@Test
	void sessionStopsWorkingAfterLogout() throws Exception {
		String csrf = value(setCookie(send("GET", "/api/me", null, null, null), "XSRF-TOKEN").orElseThrow());
		String csrfCookie = "XSRF-TOKEN=" + csrf;
		String sessionId = value(
			setCookie(send("POST", "/api/auth/login", csrfCookie, csrf, loginForm()), "JSESSIONID").orElseThrow()
		);

		HttpResponse<String> logout = send(
			"POST",
			"/api/auth/logout",
			csrfCookie + "; JSESSIONID=" + sessionId,
			csrf,
			null
		);
		assertThat(logout.statusCode()).isEqualTo(204);
		// 就算有人留著舊的 Session ID，登出後也不能再用
		assertThat(send("GET", "/api/me", "JSESSIONID=" + sessionId, null, null).statusCode()).isEqualTo(401);
	}

	@Test
	void cookiesAreSecureOnlyWhenTheOriginalRequestWasHttps() throws Exception {
		HttpResponse<String> overHttps = send("GET", "/api/me", null, null, null, true);
		String csrf = value(setCookie(overHttps, "XSRF-TOKEN").orElseThrow());
		assertThat(setCookie(overHttps, "XSRF-TOKEN").orElseThrow()).contains("Secure");

		HttpResponse<String> login = send("POST", "/api/auth/login", "XSRF-TOKEN=" + csrf, csrf, loginForm(), true);
		assertThat(login.statusCode()).isEqualTo(204);
		assertThat(setCookie(login, "JSESSIONID").orElseThrow()).contains("Secure");

		// 開發模式直接走 HTTP，帶 Secure 的 Cookie 瀏覽器不會送回來，所以這時不能加
		HttpResponse<String> overHttp = send("GET", "/api/me", null, null, null);
		assertThat(setCookie(overHttp, "XSRF-TOKEN").orElseThrow()).doesNotContain("Secure");
	}
}
