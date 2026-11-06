package io.github.hsuanc0921.favorites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 連本機 MySQL，用 DB 腳本建好的 seed 資料，只讀不寫（寫入的請求都會被擋下）
 * 跑之前要先照 DB/README.md 建好資料庫
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTests {

	private static final String USER_A = "A1236456789";
	private static final String USER_B = "B2345678901";
	private static final String USER_C = "C3456789012";

	@Autowired
	private MockMvc mvc;

	private MockHttpSession login(String userId, String password) throws Exception {
		MvcResult result = mvc
			.perform(post("/api/auth/login").with(csrf()).param("userId", userId).param("password", password))
			.andExpect(status().isNoContent())
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private MockHttpSession loginA() throws Exception {
		return login(USER_A, "DemoUserA#2026");
	}

	@Test
	void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
		String wrongPassword = mvc
			.perform(post("/api/auth/login").with(csrf()).param("userId", USER_A).param("password", "wrong"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String unknownUser = mvc
			.perform(post("/api/auth/login").with(csrf()).param("userId", "NOBODY").param("password", "wrong"))
			.andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(unknownUser).isEqualTo(wrongPassword);
	}

	@Test
	void sqlInjectionInLoginIsTreatedAsPlainText() throws Exception {
		for (String userId : new String[] { "' OR '1'='1", "A1236456789' -- ", "x'; DROP TABLE app_user; --" }) {
			mvc.perform(
				post("/api/auth/login").with(csrf()).param("userId", userId).param("password", "DemoUserA#2026")
			).andExpect(status().isUnauthorized());
		}
		// 資料表仍在，正常帳號仍能登入
		loginA();
	}

	@Test
	void otherUsersFavoriteLooksLikeItDoesNotExist() throws Exception {
		MockHttpSession b = login(USER_B, "DemoUserB#2026");

		String othersRecord = mvc
			.perform(get("/api/favorites/1").session(b))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String missingRecord = mvc
			.perform(get("/api/favorites/99999").session(b))
			.andExpect(status().isNotFound())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(othersRecord).isEqualTo(missingRecord);
	}

	@Test
	void notLoggedInGets401() throws Exception {
		mvc.perform(get("/api/favorites"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void writeWithoutCsrfTokenIsRejected() throws Exception {
		mvc.perform(
			post("/api/favorites").session(loginA()).contentType(MediaType.APPLICATION_JSON).content(favoriteJson(""))
		)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void smugglingAnotherUserIdIsRejected() throws Exception {
		// 使用者 ID 只能取自登入狀態，請求裡多帶的欄位一律不收
		mvc.perform(
			post("/api/favorites")
				.with(csrf())
				.session(loginA())
				.contentType(MediaType.APPLICATION_JSON)
				.content(favoriteJson(",\"userId\":\"" + USER_C + "\""))
		)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void otherUsersFavoriteCannotBeUpdatedOrDeleted() throws Exception {
		MockHttpSession b = login(USER_B, "DemoUserB#2026");

		// 紀錄 1 屬於使用者 A，B 改或刪都跟資料不存在一樣
		mvc.perform(
			put("/api/favorites/1")
				.with(csrf())
				.session(b)
				.header("If-Match", "\"0\"")
				.contentType(MediaType.APPLICATION_JSON)
				.content(favoriteJson(""))
		)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
		mvc.perform(delete("/api/favorites/1").with(csrf()).session(b).header("If-Match", "\"0\"")).andExpect(
			status().isNotFound()
		);

		mvc.perform(get("/api/favorites/1").session(loginA())).andExpect(status().isOk());
	}

	@Test
	void accountAlreadyRegisteredCannotBeAddedAgain() throws Exception {
		// 自己已有的帳號、別人的帳號，回應一樣，不透露帳號屬於誰
		String own = mvc
			.perform(
				post("/api/me/accounts")
					.with(csrf())
					.session(loginA())
					.contentType(MediaType.APPLICATION_JSON)
					.content(accountJson("1111999666"))
			)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ACCOUNT_TAKEN"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String others = mvc
			.perform(
				post("/api/me/accounts")
					.with(csrf())
					.session(login(USER_C, "DemoUserC#2026"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(accountJson("1111999666"))
			)
			.andExpect(status().isConflict())
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(others).isEqualTo(own);
	}

	@Test
	void accountWithNonDigitsIsRejected() throws Exception {
		mvc.perform(
			post("/api/me/accounts")
				.with(csrf())
				.session(loginA())
				.contentType(MediaType.APPLICATION_JSON)
				.content(accountJson("1111-999666"))
		)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	private static String accountJson(String account) {
		return """
		{"account":"%s","accountName":"測試帳戶"}""".formatted(account);
	}

	private static String favoriteJson(String extraFields) {
		return """
		{"productName":"測試","price":"10.00","feeRate":"0.001","purchaseQuantity":1,"account":"1111999666"%s}""".formatted(
			extraFields
		);
	}
}
