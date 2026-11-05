package io.github.hsuanc0921.favorites.common;

/** 紀錄不存在，或不屬於目前登入的使用者 */
public class FavoriteNotFoundException extends RuntimeException {

	public FavoriteNotFoundException() {
		super("favorite not found");
	}
}
