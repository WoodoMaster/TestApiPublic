package com.example.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class SignatureUtil {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Generates an HmacSHA256 signature for the given data and key.
     *
     * @param data The data to sign (query string parameters).
     * @param key  The secret key.
     * @return The lowercase hexadecimal representation of the signature.
     * @throws NoSuchAlgorithmException If HmacSHA256 is not available.
     * @throws InvalidKeyException      If the key is invalid.
     */
    public static String generateSignature(String data, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance(ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        hmac.init(secretKeySpec);
        byte[] signatureBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(signatureBytes).toLowerCase();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
