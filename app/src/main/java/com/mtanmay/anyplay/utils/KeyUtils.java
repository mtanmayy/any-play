package com.mtanmay.anyplay.utils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import static com.mtanmay.anyplay.Constants.AES;
import static com.mtanmay.anyplay.Constants.OUTPUT_KEY_LENGTH;

/**
 * Utility class that generates a random secret key for encryption/decryption purposes
 */
public class KeyUtils {

    private static final String TAG = "KeyUtils";

    /**
     * @return randomly generated secret key
     */
    public static SecretKey generateKey() {

        SecureRandom random = new SecureRandom();
        KeyGenerator keyGen = null;

        try {
            keyGen = KeyGenerator.getInstance(AES);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        keyGen.init(OUTPUT_KEY_LENGTH, random);
        SecretKey key = keyGen.generateKey();
        return key;
    }

}
