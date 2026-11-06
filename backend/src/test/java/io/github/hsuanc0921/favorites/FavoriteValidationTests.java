package io.github.hsuanc0921.favorites;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 新增喜好時，每個欄位超出範圍都要回 400，前端擋過的規則後端也要擋
 * <p>
 * 用使用者 A 登入、扣款帳號填 C 的：就算某條規則漏掉，請求也只會在帳號檢查回 422，不會真的寫進資料庫
 */
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteValidationTests {

	@Autowired
	private MockMvc mvc;

	static Stream<Arguments> invalidFields() {
		return Stream.of(
			arguments("數量帶小數", "purchaseQuantity", "1.5"),
			arguments("數量 0", "purchaseQuantity", "0"),
			arguments("數量超過 100 萬", "purchaseQuantity", "1000001"),
			arguments("價格 0", "price", "\"0.00\""),
			arguments("價格 3 位小數", "price", "\"10.005\""),
			arguments("價格整數超過 10 位", "price", "\"10000000000.00\""),
			arguments("費率超過 100%", "feeRate", "\"1.00000001\""),
			arguments("費率負數", "feeRate", "\"-0.001\""),
			arguments("費率 9 位小數", "feeRate", "\"0.000000001\""),
			arguments("沒填產品名稱", "productName", "null"),
			arguments("產品名稱只有空白", "productName", "\"   \""),
			arguments("產品名稱 101 字", "productName", "\"" + "測".repeat(101) + "\""),
			arguments("備註 201 字", "note", "\"" + "測".repeat(201) + "\""),
			arguments("帳號有非數字", "account", "\"3333-777444\""),
			arguments("帳號空白", "account", "\"\""),
			arguments("帳號超過 32 碼", "account", "\"" + "1".repeat(33) + "\"")
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidFields")
	void outOfRangeFieldIsRejected(String description, String field, String jsonValue) throws Exception {
		MockHttpSession a = (MockHttpSession) mvc
			.perform(
				post("/api/auth/login").with(csrf()).param("userId", "A1236456789").param("password", "DemoUserA#2026")
			)
			.andExpect(status().isNoContent())
			.andReturn()
			.getRequest()
			.getSession(false);

		mvc.perform(
			post("/api/favorites")
				.with(csrf())
				.session(a)
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBodyWith(field, jsonValue))
		)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	/** 其他欄位都合法，只把指定的欄位換掉 */
	private static String validBodyWith(String field, String jsonValue) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("productName", "\"測試\"");
		body.put("price", "\"10.00\"");
		body.put("feeRate", "\"0.001\"");
		body.put("purchaseQuantity", "1");
		body.put("account", "\"3333777444\"");
		body.put("note", "\"\"");
		body.put(field, jsonValue);
		return body
			.entrySet()
			.stream()
			.map(e -> "\"" + e.getKey() + "\":" + e.getValue())
			.collect(Collectors.joining(",", "{", "}"));
	}
}
