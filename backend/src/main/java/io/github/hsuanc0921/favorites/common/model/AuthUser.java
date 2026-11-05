package io.github.hsuanc0921.favorites.common.model;

/** 登入驗證用的帳號資料，只在登入時使用 */
public record AuthUser(String userId, String passwordHash) {}
