package com.example.legacy.util;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public final class Passwords {
    private static final int ITERATIONS = 150000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private Passwords() {}
    public static String hash(String password) {
        if (password == null || password.length() < 12) throw new IllegalArgumentException("Password must be at least 12 characters");
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        byte[] digest = derive(password, salt, ITERATIONS);
        return "PBKDF2_SHA256$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(digest);
    }
    public static boolean matches(String password, String encoded) {
        try {
            String[] p = encoded.split("\\$");
            if (p.length != 4 || !"PBKDF2_SHA256".equals(p[0])) return false;
            int iterations = Integer.parseInt(p[1]);
            if (iterations < 10000 || iterations > 1000000) return false;
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] expected = Base64.getDecoder().decode(p[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (Exception e) { return false; }
    }
    private static byte[] derive(String password, byte[] salt, int iterations) {
        char[] chars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (Exception e) { throw new IllegalStateException("PBKDF2 unavailable", e); }
        finally { spec.clearPassword(); java.util.Arrays.fill(chars, '\0'); }
    }
}
