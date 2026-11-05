package io.github.hsuanc0921.favorites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
}
