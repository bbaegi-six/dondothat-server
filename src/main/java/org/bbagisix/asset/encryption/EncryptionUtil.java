package org.bbagisix.asset.encryption;

import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// 암호화
@Component
public class EncryptionUtil {
	// RSA 관련 상수
	private static final String RSA_ALGORITHM = "RSA";
	private static final String RSA_CIPHER = "RSA/ECB/PKCS1Padding";

	// AES 관련 상수
	private static final String AES_ALGORITHM = "AES";
	private static final String AES_CIPHER = "AES/CBC/PKCS5Padding";
	private static final int AES_KEY_LENGTH = 256;
	private static final int AES_IV_LENGTH = 16;
	private static final String ENCRYPTION_PREFIX = "ENC:"; // 암호화 데이터 식별용

	@Value("${AES_SECRET_KEY}")
	private String aesBase64SecretKey;


	// RSA 암호화
	public String encryptRSA(String plainText, String rsaBase64PublicKey) {
		try {
			byte[] bytePublicKey = Base64.getDecoder().decode(rsaBase64PublicKey);
			KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
			PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(bytePublicKey));

			Cipher cipher = Cipher.getInstance(RSA_CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] bytePlain = cipher.doFinal(plainText.getBytes());
			return Base64.getEncoder().encodeToString(bytePlain);
		} catch (NoSuchPaddingException err) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL, "RSA 패딩 방식을 찾을 수 없습니다: " + err.getMessage());
		} catch (IllegalBlockSizeException err) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL, "RSA 암호화 블록 크기가 유효하지 않습니다: " + err.getMessage());
		} catch (NoSuchAlgorithmException err) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL, "RSA 알고리즘을 찾을 수 없습니다: " + err.getMessage());
		} catch (InvalidKeySpecException | InvalidKeyException err) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL, "RSA 암호화 키가 유효하지 않습니다: " + err.getMessage());
		} catch (BadPaddingException err) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL, "RSA 암호화 패딩 처리 중 오류가 발생했습니다: " + err.getMessage());
		}
	}

	// AES 키 생성
	public static String generateAESKey() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
			keyGenerator.init(AES_KEY_LENGTH);
			SecretKey secretKey = keyGenerator.generateKey();

			// 원시 바이트 -> base64 인코딩 -> String
			return  Base64.getEncoder().encodeToString(secretKey.getEncoded());

		} catch (NoSuchAlgorithmException err) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL, "AES 알고리즘을 찾을 수 없습니다: " + err.getMessage());
		}
	}

	// Base64 문자열을 SecretKey로 변환
	private SecretKey base64ToKey(String base64KeyString) {
		if(!StringUtils.hasText(base64KeyString)){
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL, "암호화 키가 설정되지 않았습니다.");
		}
		try{
			byte[] keyBytes = Base64.getDecoder().decode(base64KeyString);
			return new SecretKeySpec(keyBytes, AES_ALGORITHM);
		} catch (IllegalArgumentException err) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL, "유효하지 않은 Base64 암호화 키 형식입니다: " + err.getMessage());
		} catch (Exception err) {
			throw new BusinessException(ErrorCode.ENCRYPTION_FAIL, "암호화 키 변환 중 오류가 발생했습니다: " + err.getMessage());
		}
	}


	// AES 암호화
	public String encryptAES (String plainText) {
		if(!StringUtils.hasText(plainText) || plainText.startsWith(ENCRYPTION_PREFIX)){
			return plainText;
		}
		try {
			SecretKey secretKey = base64ToKey(aesBase64SecretKey);

			// IV 생성
			byte[] iv = new byte[AES_IV_LENGTH];
			new SecureRandom().nextBytes(iv);
			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			// cipher
			Cipher cipher = Cipher.getInstance(AES_CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

			byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

			// IV + 암호화된 데이터를 결합
			byte[] encryptedWithIv = new byte[AES_IV_LENGTH + encryptedBytes.length];
			System.arraycopy(iv, 0, encryptedWithIv, 0, AES_IV_LENGTH);
			System.arraycopy(encryptedBytes, 0, encryptedWithIv, AES_IV_LENGTH, encryptedBytes.length);

			// 접두사 붙여서 반환
			return ENCRYPTION_PREFIX + Base64.getEncoder().encodeToString(encryptedWithIv);
		} catch (NoSuchAlgorithmException err) {
			throw new RuntimeException(err);
		} catch (NoSuchPaddingException err) {
			throw new RuntimeException(err);
		} catch (IllegalBlockSizeException err) {
			throw new RuntimeException(err);
		} catch (UnsupportedEncodingException err) {
			throw new RuntimeException(err);
		} catch (InvalidAlgorithmParameterException err) {
			throw new RuntimeException(err);
		} catch (BadPaddingException err) {
			throw new RuntimeException(err);
		} catch (InvalidKeyException err) {
			throw new RuntimeException(err);
		}
	}

	// Static AES 복호화 메서드
	public String decryptAES(String encryptedText) {
		// 빈 문자열이거나 암호화 접두사가 없는 문자열은 그대로 반환
		if (!StringUtils.hasText(encryptedText) || !encryptedText.startsWith(ENCRYPTION_PREFIX)) {
			return encryptedText;
		}
		try {
			SecretKey secretKey = base64ToKey(aesBase64SecretKey);
			if (secretKey == null) {
				return encryptedText;
			}

			// 접두사 제거 후 Base64로 디코딩
			String base64Data = encryptedText.substring(ENCRYPTION_PREFIX.length());
			byte[] encryptedWithIv = Base64.getDecoder().decode(base64Data);

			// IV 추출
			byte[] iv = new byte[AES_IV_LENGTH];
			System.arraycopy(encryptedWithIv, 0, iv, 0, AES_IV_LENGTH);
			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			// 암호화된 데이터 추출
			byte[] encryptedBytes = new byte[encryptedWithIv.length - AES_IV_LENGTH];
			System.arraycopy(encryptedWithIv, AES_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

			Cipher cipher = Cipher.getInstance(AES_CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

			byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
			return new String(decryptedBytes, "UTF-8");
		} catch (InvalidAlgorithmParameterException err) {
			throw new RuntimeException(err);
		} catch (NoSuchPaddingException err) {
			throw new RuntimeException(err);
		} catch (IllegalBlockSizeException err) {
			throw new RuntimeException(err);
		} catch (UnsupportedEncodingException err) {
			throw new RuntimeException(err);
		} catch (NoSuchAlgorithmException err) {
			throw new RuntimeException(err);
		} catch (BadPaddingException err) {
			throw new RuntimeException(err);
		} catch (InvalidKeyException err) {
			throw new RuntimeException(err);
		}
	}
}
