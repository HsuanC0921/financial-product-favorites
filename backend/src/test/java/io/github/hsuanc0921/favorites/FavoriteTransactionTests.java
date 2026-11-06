package io.github.hsuanc0921.favorites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.hsuanc0921.favorites.repository.FavoriteRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 驗證新增時 Product 與 Like List 在同一個交易裡，失敗會整筆回復
 * <p>
 * 應用程式的資料庫帳號沒有讀資料表的權限，查不到 product 有沒有殘留，
 * 所以改從 Spring 這邊確認：預存程序是在交易中執行的，而且這個交易最後被回復
 */
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteTransactionTests {

	@Autowired
	private MockMvc mvc;

	@MockitoSpyBean
	private FavoriteRepository repository;

	@Test
	void failedInsertIntoLikeListRollsBackTheProduct() throws Exception {
		List<Boolean> inTransaction = new ArrayList<>();
		List<Integer> outcome = new ArrayList<>();
		doAnswer(invocation -> {
			inTransaction.add(TransactionSynchronizationManager.isActualTransactionActive());
			TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCompletion(int status) {
						outcome.add(status);
					}
				}
			);
			return invocation.callRealMethod();
		})
			.when(repository)
			.create(anyString(), any(), any(), any());

		MockHttpSession c = (MockHttpSession) mvc
			.perform(
				post("/api/auth/login").with(csrf()).param("userId", "C3456789012").param("password", "DemoUserC#2026")
			)
			.andExpect(status().isNoContent())
			.andReturn()
			.getRequest()
			.getSession(false);

		// 1111999666 是使用者 A 的帳號：Product 先寫入，接著 Like List 的外鍵檢查失敗
		mvc.perform(
			post("/api/favorites")
				.with(csrf())
				.session(c)
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{"productName":"不該留下的商品","price":"10.00","feeRate":"0.001","purchaseQuantity":1,"account":"1111999666"}"""
				)
		)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("UNKNOWN_ACCOUNT"));

		assertThat(inTransaction).containsExactly(true);
		assertThat(outcome).containsExactly(TransactionSynchronization.STATUS_ROLLED_BACK);
	}
}
