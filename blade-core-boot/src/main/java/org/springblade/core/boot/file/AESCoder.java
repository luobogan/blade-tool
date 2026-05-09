package org.springblade.core.boot.file;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * AES加密工具类
 * 参考ecology项目实现
 *
 * @author Blade
 */
@Slf4j
public class AESCoder {

	private static final String KEY_ALGORITHM = "AES";
	private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

	/**
	 * 初始化密钥
	 *
	 * @param code 密码
	 * @return byte[] 密钥
	 */
	public static byte[] initSecretKey(String code) {
		try {
			KeyGenerator kg = KeyGenerator.getInstance(KEY_ALGORITHM);
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(code.getBytes());
			kg.init(128, secureRandom);
			SecretKey secretKey = kg.generateKey();
			return secretKey.getEncoded();
		} catch (Exception e) {
			log.error("初始化AES密钥失败", e);
			return new byte[0];
		}
	}

	/**
	 * 转换密钥
	 */
	private static SecretKeySpec toKey(byte[] key) {
		return new SecretKeySpec(key, KEY_ALGORITHM);
	}

	/**
	 * 加密文件
	 *
	 * @param inputPath  输入文件路径
	 * @param outputPath 输出文件路径
	 * @param code       密码
	 */
	public static void encryptFile(String inputPath, String outputPath, String code) {
		try (FileInputStream fis = new FileInputStream(inputPath);
			 FileOutputStream fos = new FileOutputStream(outputPath)) {
			OutputStream encryptOut = encrypt(fos, code);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = fis.read(buffer)) != -1) {
				encryptOut.write(buffer, 0, len);
			}
			encryptOut.flush();
			encryptOut.close();
		} catch (Exception e) {
			log.error("加密文件失败: {}", inputPath, e);
		}
	}

	/**
	 * 解密文件
	 *
	 * @param inputPath  输入文件路径（加密文件）
	 * @param outputPath 输出文件路径（解密后）
	 * @param code       密码
	 */
	public static void decryptFile(String inputPath, String outputPath, String code) {
		try (FileInputStream fis = new FileInputStream(inputPath);
			 FileOutputStream fos = new FileOutputStream(outputPath)) {
			InputStream decryptIn = decrypt(fis, code);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = decryptIn.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}
			decryptIn.close();
		} catch (Exception e) {
			log.error("解密文件失败: {}", inputPath, e);
		}
	}

	/**
	 * 获取加密输出流
	 *
	 * @param out  输出流
	 * @param code 密码
	 * @return CipherOutputStream
	 */
	public static OutputStream encrypt(OutputStream out, String code) throws Exception {
		byte[] key = initSecretKey(code);
		SecretKeySpec k = toKey(key);
		Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, k);
		return new CipherOutputStream(out, cipher);
	}

	/**
	 * 获取解密输入流
	 *
	 * @param in   输入流
	 * @param code 密码
	 * @return CipherInputStream
	 */
	public static InputStream decrypt(InputStream in, String code) throws Exception {
		byte[] key = initSecretKey(code);
		SecretKeySpec k = toKey(key);
		Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, k);
		return new CipherInputStream(in, cipher);
	}

	/**
	 * 生成随机密钥（13位）
	 *
	 * @return 密钥字符串
	 */
	public static String generateRandomKey() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 13);
	}

}