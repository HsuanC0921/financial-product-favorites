package io.github.hsuanc0921.favorites.common;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * 登入、登出、CSRF 與 Session 的安全設定
 * <ul>
 * <li>除登入與錯誤頁外，所有 API 都要先登入</li>
 * <li>登入用表單格式送出，成功回 204，Session Cookie 為 HttpOnly</li>
 * <li>CSRF 採「Cookie 加標頭」：伺服器發一個前端腳本讀得到的 XSRF-TOKEN Cookie，
 * 前端把同一個值放進 X-XSRF-TOKEN 標頭，兩者不符回 403</li>
 * <li>未登入與被拒絕一律回固定格式的 JSON，不導向登入頁</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// 前端要讀這個 Cookie 再放進標頭，所以不能是 HttpOnly
		CookieCsrfTokenRepository csrfRepository = new CookieCsrfTokenRepository();
		csrfRepository.setCookieCustomizer(cookie -> cookie.httpOnly(false).sameSite("Lax"));

		http.csrf(csrf ->
			csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
		)
			.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
			// 未登入的請求不建立 Session，避免被拿來耗用伺服器記憶體
			.requestCache(cache -> cache.requestCache(new NullRequestCache()))
			.authorizeHttpRequests(auth -> auth.requestMatchers("/error").permitAll().anyRequest().authenticated())
			// 登入頁與處理網址相同，這樣 Spring 不會另外產生預設登入頁
			.formLogin(login ->
				login
					.loginPage("/api/auth/login")
					.loginProcessingUrl("/api/auth/login")
					.usernameParameter("userId")
					.passwordParameter("password")
					.successHandler((request, response, authentication) ->
						response.setStatus(HttpStatus.NO_CONTENT.value())
					)
					.failureHandler((request, response, exception) ->
						writeError(response, HttpStatus.UNAUTHORIZED, ApiError.LOGIN_FAILED)
					)
					.permitAll()
			)
			.logout(logout ->
				logout
					.logoutUrl("/api/auth/logout")
					.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
			)
			.exceptionHandling(handling ->
				handling
					.authenticationEntryPoint((request, response, exception) ->
						writeError(response, HttpStatus.UNAUTHORIZED, ApiError.UNAUTHENTICATED)
					)
					.accessDeniedHandler((request, response, exception) ->
						writeError(response, HttpStatus.FORBIDDEN, ApiError.FORBIDDEN)
					)
			);
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		// 強度要和資料庫裡雜湊的一致（12），帳號不存在時 Spring 用同強度的假雜湊比對，兩種情況耗時才相同
		return new BCryptPasswordEncoder(12);
	}

	private static void writeError(HttpServletResponse response, HttpStatus status, ApiError error) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(error.toJson());
	}
}
