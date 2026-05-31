package com.recipe.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_BYTES = 16;

    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        byte[] salt = new byte[SALT_BYTES];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        byte[] digest = digest(salt, password);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(digest);
    }

    public static boolean check(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split(":");
        if (parts.length != 2) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        byte[] actual = digest(salt, password);

        if (actual.length != expected.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < actual.length; i++) {
            result |= actual[i] ^ expected[i];
        }
        return result == 0;
    }

    private static byte[] digest(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            md.update(password.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not available: " + HASH_ALGORITHM, e);
        }
    }
}
