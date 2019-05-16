package de.unibremen.beduino.dcaf;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static org.apache.commons.codec.CharEncoding.UTF_8;

/**
 * Implementation von https://stackoverflow.com/a/53403527
 *
 * CBC-Mode
 * SHA-256 für den Passworthash
 * Verschlüsselte Ausgabe als Base64 (davon 0.-24. Byte IV)
 *
 * Bei Illegal KeySize Error siehe: https://stackoverflow.com/a/6481658
 */

public class AES {
    private static SecretKeySpec getKeySpec(String passphrase) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        return new SecretKeySpec(digest.digest(passphrase.getBytes(UTF_8)), "AES");
    }

    private static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance("AES/CBC/PKCS5PADDING");
    }

    static String encrypt(String passphrase, String value) throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] initVector = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(initVector);
        Cipher cipher = getCipher();
        cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(passphrase), new IvParameterSpec(initVector));
        byte[] encrypted = cipher.doFinal(value.getBytes());

        return DatatypeConverter.printBase64Binary(initVector) + DatatypeConverter.printBase64Binary(encrypted);
    }

    static String decrypt(String passphrase, String encrypted) throws GeneralSecurityException, UnsupportedEncodingException {
        byte[] initVector = DatatypeConverter.parseBase64Binary(encrypted.substring(0, 24));
        Cipher cipher = getCipher();
        cipher.init(Cipher.DECRYPT_MODE, getKeySpec(passphrase), new IvParameterSpec(initVector));
        byte[] original = cipher.doFinal(DatatypeConverter.parseBase64Binary(encrypted.substring(24)));

        return new String(original);
    }
}
