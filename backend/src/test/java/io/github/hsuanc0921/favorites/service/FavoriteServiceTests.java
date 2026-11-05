package io.github.hsuanc0921.favorites.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.hsuanc0921.favorites.common.model.Favorite;
import io.github.hsuanc0921.favorites.common.model.FavoritePage;
import io.github.hsuanc0921.favorites.repository.FavoriteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 不連資料庫，只驗服務層自己的規則 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceTests {

	@Mock
	private FavoriteRepository repository;

	@InjectMocks
	private FavoriteService service;

	@Nested
	class Paging {

		@Test
		void fullPageGivesTheLastSnToContinueFrom() {
			when(repository.list("A", 0)).thenReturn(favorites(1, FavoriteService.PAGE_SIZE));

			FavoritePage page = service.list("A", 0);

			assertThat(page.items()).hasSize(FavoriteService.PAGE_SIZE);
			assertThat(page.nextAfterSn()).isEqualTo(FavoriteService.PAGE_SIZE);
		}

		@Test
		void shortPageMeansThereIsNothingMore() {
			when(repository.list("A", 0)).thenReturn(favorites(1, FavoriteService.PAGE_SIZE - 1));

			assertThat(service.list("A", 0).nextAfterSn()).isNull();
		}

		@Test
		void emptyPageMeansThereIsNothingMore() {
			when(repository.list("A", 500)).thenReturn(List.of());

			assertThat(service.list("A", 500).nextAfterSn()).isNull();
		}

		private static List<Favorite> favorites(long fromSn, int count) {
			return LongStream.range(fromSn, fromSn + count)
				.mapToObj(sn ->
					new Favorite(
						sn,
						"商品" + sn,
						BigDecimal.ONE,
						BigDecimal.ZERO,
						1,
						"0000000000",
						BigDecimal.ZERO,
						BigDecimal.ONE,
						null,
						Instant.EPOCH,
						0
					)
				)
				.toList();
		}
	}
}
