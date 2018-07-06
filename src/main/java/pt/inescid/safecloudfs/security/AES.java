package pt.inescid.safecloudfs.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

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
		try {
//			byte[] result;
			int opMode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
			AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivData);

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", new BouncyCastleProvider());
			cipher.init(opMode, secretKey, ivSpec);


			byte[] inputBuffer = new byte[ payload.length ];
			InputStream in = new ByteArrayInputStream(payload);
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			int r = in.read(inputBuffer);
			System.out.println("r = " + r);
			while (r >= 0) {
				byte[] outputUpdate = cipher.update(inputBuffer, 0, r);
				out.write(outputUpdate);
				r = in.read(inputBuffer);
			}
			byte[] outputFinalUpdate = cipher.doFinal();

			System.out.println("outputFinalUpdate length = " + outputFinalUpdate.length);
			out.write( outputFinalUpdate );
			System.out.println("outputFinalUpdate length = " + outputFinalUpdate.length);
			return out.toByteArray();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

//	public static void main(String[] args) throws InvalidVSSScheme {
//
//		byte[] bytes = "This is a new file\n".getBytes();
//
//		byte[][] e = ErasureCodes.encode(bytes, 3, 1);
//
//		byte[] d = ErasureCodes.decode(e, 3, 1);
//
//		System.out.println(
//				Integer.BYTES + "Test: " + Arrays.equals(bytes, d) + " bytes =" + bytes.length + " d = " + d.length);
//
//		for (int i = 0; i < d.length; i++) {
//			System.out.println("" + i + ": " + bytes[Math.min(i, bytes.length - 1)] + " - " + d[i]);
//		}
//
//		SecretKey secretKey = getSecretKey();
//		String message = "This is a new file\n";
//		bytes = message.getBytes();
//		byte[] encrypted = encrypt(bytes, secretKey);
//
//		System.out.println("Size encrypted, befor EC= " + encrypted.length);
//
//		System.out.println("Decrypted before EC=" + new String(decrypt(encrypted, secretKey)));
//
//		System.out.println("Le encrypted: " + new String(Base64.getEncoder().encode(encrypted)));
//
//		byte[][] encoded = ErasureCodes.encode(encrypted, 3, 1);
//
//		byte[] decoded = ErasureCodes.decode(encoded, 3, 1);
//
//		System.out.println("Le join: " + new String(Base64.getEncoder().encode(decoded)));
//
//		byte[] decrypted = decrypt(decoded, secretKey);
//
//		System.out.println("Decrypted = " + new String(decrypted));
//	}
//
}
