/**
 * AES-256-GCM 加密工具 — 用于 API Key 安全存储
 *
 * 密钥优先级:
 *   1. 环境变量 APP_ENCRYPTION_KEY（Base64 编码的 256-bit 密钥，生产环境必须设置）
 *   2. 从 application.yml 的 jwt.secret 派生（开发环境，重启不丢失）
 *
 * 生成随机密钥命令: openssl rand -base64 32
 */
package org.example.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class AesUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /** 默认密钥材料（硬编码 + JWT secret 拼接，确保重启后一致） */
    private static final String DEFAULT_KEY_MATERIAL =
            "CodeForge@2026!AES-256-GCM-Key-Material-v1.0";

    private static volatile SecretKey cachedKey;

    private static SecretKey getKey() {
        if (cachedKey != null) return cachedKey;
        // 1. 优先环境变量
        String envKey = System.getenv("APP_ENCRYPTION_KEY");
        if (envKey != null && !envKey.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(envKey);
            cachedKey = new SecretKeySpec(decoded, ALGORITHM);
            return cachedKey;
        }
        // 2. 从 JWT secret 派生（持久化，开发环境用）
        String jwtSecret = System.getProperty("jwt.secret",
                System.getenv("JWT_SECRET") != null ? System.getenv("JWT_SECRET") : DEFAULT_KEY_MATERIAL);
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] derived = sha.digest((DEFAULT_KEY_MATERIAL + ":" + jwtSecret).getBytes());
            cachedKey = new SecretKeySpec(Arrays.copyOf(derived, 32), ALGORITHM);
            return cachedKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }

    /** Encrypt plaintext → Base64(IV + ciphertext) */
    public static String encrypt(String plaintext) {
        try {
            SecretKey key = getKey();
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes());
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /** Decrypt Base64(IV + ciphertext) → plaintext */
    public static String decrypt(String encrypted) {
        try {
            SecretKey key = getKey();
            byte[] combined = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    /** Mask API key for display: sk-a***b123 */
    public static String mask(String rawKey) {
        if (rawKey == null || rawKey.length() <= 8) return "****";
        return rawKey.substring(0, 3) + "***" + rawKey.substring(rawKey.length() - 4);
    }
}
