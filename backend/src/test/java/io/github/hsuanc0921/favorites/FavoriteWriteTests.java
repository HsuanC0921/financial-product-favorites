package io.github.hsuanc0921.favorites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 連本機 MySQL 的寫入測試，用 seed 裡沒有任何紀錄的使用者 C
 * 建立的紀錄都由測試自己刪掉，不留資料
 */
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteWriteTests {

	private static final String C_ACCOUNT = "3333777444";

	@Autowired
	private MockMvc mvc;

	private MockHttpSession loginAs(String userId, String password) throws Exception {
		MvcResult result = mvc
			.perform(post("/api/auth/login").with(csrf()).param("userId", userId).param("password", password))
			.andExpect(status().isNoContent())
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private MockHttpSession loginC() throws Exception {
		return loginAs("C3456789012", "DemoUserC#2026");
	}

	private static String body(String name, String price, String rate, int qty, String account, String note) {
		return """
		{"productName":"%s","price":"%s","feeRate":"%s","purchaseQuantity":%d,"account":"%s","note":"%s"}""".formatted(
			name,
			price,
			rate,
			qty,
			account,
			note
		);
	}

	private static String body(String price, String rate, int qty) {
		return body("測試商品 ETF", price, rate, qty, C_ACCOUNT, "");
	}

	private MockHttpServletRequestBuilder json(
		MockHttpServletRequestBuilder builder,
		MockHttpSession session,
		String content
	) {
		return builder.with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON).content(content);
	}

	private long create(MockHttpSession session, String content) throws Exception {
		String location = mvc
			.perform(json(post("/api/favorites"), session, content))
			.andExpect(status().isCreated())
			.andExpect(header().string("ETag", "\"0\""))
			.andReturn()
			.getResponse()
			.getHeader("Location");
		return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
	}

	private void remove(MockHttpSession session, long sn, int version) throws Exception {
		mvc.perform(
			delete("/api/favorites/" + sn)
				.with(csrf())
				.session(session)
				.header("If-Match", "\"" + version + "\"")
		).andExpect(status().isNoContent());
	}

	@Test
	void createUpdateDeleteRoundTrip() throws Exception {
		MockHttpSession c = loginC();

		// 範例數字：45.60 × 1000，費率 0.1425%，手續費 64.98
		long sn = create(c, body("心悅台灣成長 ETF", "45.60", "0.001425", 1000, C_ACCOUNT, "新增測試"));
		mvc.perform(get("/api/favorites/" + sn).session(c))
			.andExpect(jsonPath("$.totalFee").value("64.98"))
			.andExpect(jsonPath("$.totalAmount").value("45664.98"))
			.andExpect(jsonPath("$.version").value(0));

		// 修改後版本加一，金額跟著重算
		mvc.perform(
			json(
				put("/api/favorites/" + sn),
				c,
				body("清和全球科技 ETF", "100.00", "0.0005", 10, C_ACCOUNT, "改過")
			).header("If-Match", "\"0\"")
		)
			.andExpect(status().isOk())
			.andExpect(header().string("ETag", "\"1\""))
			.andExpect(jsonPath("$.productName").value("清和全球科技 ETF"))
			.andExpect(jsonPath("$.totalFee").value("0.50"))
			.andExpect(jsonPath("$.totalAmount").value("1000.50"))
			.andExpect(jsonPath("$.note").value("改過"));

		// 拿舊版本改、刪都被擋，沒帶 If-Match 則要求補上
		mvc.perform(json(put("/api/favorites/" + sn), c, body("100.00", "0", 1)).header("If-Match", "\"0\""))
			.andExpect(status().isPreconditionFailed())
			.andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
		mvc.perform(
			delete("/api/favorites/" + sn)
				.with(csrf())
				.session(c)
				.header("If-Match", "\"0\"")
		).andExpect(status().isPreconditionFailed());
		mvc.perform(json(put("/api/favorites/" + sn), c, body("100.00", "0", 1)))
			.andExpect(status().isPreconditionRequired())
			.andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
		mvc.perform(
			delete("/api/favorites/" + sn)
				.with(csrf())
				.session(c)
				.header("If-Match", "abc")
		).andExpect(status().isBadRequest());

		remove(c, sn, 1);
		mvc.perform(get("/api/favorites/" + sn).session(c)).andExpect(status().isNotFound());
	}

	@Test
	void sameVersionOnlyOneWriterWins() throws Exception {
		MockHttpSession c = loginC();
		long sn = create(c, body("10.00", "0", 1));

		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			List<Callable<Integer>> writers = List.of(
				() -> raceUpdate(c, sn, "甲 ETF", start),
				() -> raceUpdate(c, sn, "乙 ETF", start)
			);
			List<Future<Integer>> results = writers.stream().map(pool::submit).toList();
			start.countDown();
			List<Integer> statuses = List.of(results.get(0).get(), results.get(1).get());
			assertThat(statuses).containsExactlyInAnyOrder(200, 412);
		} finally {
			pool.shutdownNow();
		}

		mvc.perform(get("/api/favorites/" + sn).session(c)).andExpect(jsonPath("$.version").value(1));
		remove(c, sn, 1);
	}

	private int raceUpdate(MockHttpSession session, long sn, String name, CountDownLatch start) throws Exception {
		start.await();
		return mvc
			.perform(
				json(put("/api/favorites/" + sn), session, body(name, "10.00", "0", 1, C_ACCOUNT, "")).header(
					"If-Match",
					"\"0\""
				)
			)
			.andReturn()
			.getResponse()
			.getStatus();
	}
}
