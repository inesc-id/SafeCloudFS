package pt.inescid.safecloudfs.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class AES {
	private static int keySize = 128; // Java does not allow more than 128
	private static String keyAlgorithm = "AES";
	private static byte[] ivData = "SafeCloudFS2018!".getBytes();

	public AES() {

	}

	public static byte[] getSalt() {
		SecureRandom random = new SecureRandom();
		byte bytes[] = new byte[20];
		random.nextBytes(bytes);
		return bytes;
	}

	public static SecretKey getSecretKeyFromBytes(byte[] key) {
		SecretKey secretKey = new SecretKeySpec(key, keyAlgorithm);
		return secretKey;
	}

	public static SecretKey getSecretKey() {
		try {
			SecureRandom secureRandom = new SecureRandom();
			byte[] key = new byte[(keySize / 8)];
			secureRandom.nextBytes(key);
			SecretKey secretKey = new SecretKeySpec(key, keyAlgorithm);
			return secretKey;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}

		return null;
	}

	/**
	 *
	 * @param plainText
	 * @return encrypted text
	 * @throws Exception
	 */
	public static byte[] encrypt(byte[] plainBytes, SecretKey secretKey) {
		if (plainBytes == null || secretKey == null) {
			return null;
		}
		return process(true, plainBytes, secretKey);
	}

	/**
	 *
	 * @param encryptText
	 * @return decrypted text
	 * @throws Exception
	 */
	public static byte[] decrypt(byte[] encryptBytes, SecretKey secretKey) {
		return process(false, encryptBytes, secretKey);

	}

	/**
	 *
	 * @param encryptText
	 * @return decrypted text
	 * @throws Exception
	 */
	public static byte[] process(boolean encrypt, byte[] payload, SecretKey secretKey) {
		byte[] prefix = "SAFECLOUDFS_PREFIX".getBytes();
		try {
			if (encrypt) {

				byte[] payloadWithPrefix = new byte[payload.length + prefix.length];

				System.arraycopy(prefix, 0, payloadWithPrefix, 0, prefix.length);
				System.arraycopy(payload, 0, payloadWithPrefix, prefix.length, payload.length);

				payload = payloadWithPrefix;
			}

			int opMode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
			AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivData);

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", new BouncyCastleProvider());
			cipher.init(opMode, secretKey, ivSpec);

			byte[] inputBuffer = new byte[payload.length];
			InputStream in = new ByteArrayInputStream(payload);
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			int r = in.read(inputBuffer);
			while (r >= 0) {
				byte[] outputUpdate = cipher.update(inputBuffer, 0, r);
				out.write(outputUpdate);
				r = in.read(inputBuffer);
			}
			byte[] outputFinalUpdate = cipher.doFinal();
			out.write(outputFinalUpdate);

			byte[] result = out.toByteArray();

			if (!encrypt) {

				byte[] resultWithoutPrefix = new byte[result.length - prefix.length];

				System.arraycopy(result,  prefix.length, resultWithoutPrefix, 0, result.length-prefix.length);
				result = resultWithoutPrefix;


			}
			return result;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

//	public static void main(String[] args) {
//
//		SecretKey sk = getSecretKey();
//		String message = "ola";
//
//		System.out.println("Message clear: " + message);
//
//
//
//
//		byte[] enc = process(true, message.getBytes(), sk);
//
//		System.out.println("Message enc: " + Base64.getEncoder().encodeToString(enc));
//
//		byte[] dec = process(false, enc, sk);
//
//		System.out.println("Message dec: " + new String(dec));
//	}

}
