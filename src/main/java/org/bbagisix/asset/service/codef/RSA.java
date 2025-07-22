package org.bbagisix.asset.service.codef;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

// RSA 암호화
public class RSA {
	private static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";
	private static final String KEY_ALGORITHM = "RSA";

	// plainText : 암호화할 text
	// base64PublicKey : codef public_key
	public static String encryptRSA(String plainText, String base64PublicKey) {
		try {
			byte[] bytePublicKey = Base64.getDecoder().decode(base64PublicKey);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(bytePublicKey));

			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] bytePlain = cipher.doFinal(plainText.getBytes());
			return Base64.getEncoder().encodeToString(bytePlain);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException |InvalidKeyException|IllegalBlockSizeException|BadPaddingException err) {
			err.printStackTrace();
			return null;
		}
	}
}
