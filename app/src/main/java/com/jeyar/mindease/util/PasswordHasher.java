package com.jeyar.mindease.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PasswordHasher {

    private static final int ITERATIONS = 10_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static String hash(String password, String saltBase64) {
        try {
            byte[] salt = Base64.decode(saltBase64, Base64.NO_WRAP);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            for (int i = 1; i < ITERATIONS; i++) {
                md.reset();
                digest = md.digest(digest);
            }
            return Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
