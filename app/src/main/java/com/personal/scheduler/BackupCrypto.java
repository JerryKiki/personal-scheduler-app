package com.personal.scheduler;

import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

final class BackupCrypto {
    private static final SecureRandom RANDOM = new SecureRandom();

    private BackupCrypto() {
    }

    static String newCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    static String encrypt(JSONObject data, String code) throws Exception {
        byte[] salt = randomBytes(16);
        byte[] iv = randomBytes(12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(code, salt), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
        JSONObject root = new JSONObject();
        root.put("format", "personal-scheduler-backup");
        root.put("version", 1);
        root.put("salt", b64(salt));
        root.put("iv", b64(iv));
        root.put("data", b64(encrypted));
        return root.toString();
    }

    static JSONObject decrypt(String backupText, String code) throws Exception {
        JSONObject root = new JSONObject(backupText);
        byte[] salt = fromB64(root.getString("salt"));
        byte[] iv = fromB64(root.getString("iv"));
        byte[] encrypted = fromB64(root.getString("data"));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(code, salt), new GCMParameterSpec(128, iv));
        byte[] plain = cipher.doFinal(encrypted);
        return new JSONObject(new String(plain, StandardCharsets.UTF_8));
    }

    private static SecretKey key(String code, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), salt, 120_000, 256);
        byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static String b64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private static byte[] fromB64(String text) {
        return Base64.decode(text, Base64.NO_WRAP);
    }
}
