package io.github.hsuanc0921.favorites.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** CSRF token 預設延遲產生，這裡每次請求都讀一次，讓前端第一次請求就拿到 Cookie */
public class CsrfCookieFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
		throws ServletException, IOException {
		CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
		if (token != null) {
			token.getToken();
		}
		chain.doFilter(request, response);
	}
}
