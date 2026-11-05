package io.github.hsuanc0921.favorites.common.model;

import java.util.List;

/** nextAfterSn 為 null 代表沒有下一頁 */
public record FavoritePage(List<Favorite> items, Long nextAfterSn) {}
