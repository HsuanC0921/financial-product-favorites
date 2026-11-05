package io.github.hsuanc0921.favorites.common.model;

/** 使用者的基本資料與預設扣款帳號 */
public record UserProfile(String userId, String userName, String email, String account) {}
