package com.mtanmay.anyplay.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.mtanmay.anyplay.Constants.AES;
import static com.mtanmay.anyplay.Constants.CIPHER_ALGORITHM;

/**
 * Utility class for encryption and decryption methods
 */
public class EncryptDecryptUtils {

    /**
     * Encrypts the given bytes using the secret key
     * @param secretKey key used to encrypt the bytes
     * @param fileData bytes to encrypt
     * @return encrypted bytes
     * @throws Exception
     */
    public static byte[] encode(byte[] secretKey, byte[] fileData)
            throws Exception {

        SecretKeySpec spec = new SecretKeySpec(secretKey, 0, secretKey.length, AES);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, spec, new IvParameterSpec(new byte[cipher.getBlockSize()]));

        return cipher.doFinal(fileData);

    }

    /**
     * Decrypts the given bytes using the secret key
     * @param secretKey key used to decrypt the bytes
     * @param fileData byte to decrypt
     * @return decrypted bytes
     * @throws Exception
     */
    public static byte[] decode(SecretKey secretKey, byte[] fileData)
            throws Exception {

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[cipher.getBlockSize()]));

        return cipher.doFinal(fileData);
    }

}
