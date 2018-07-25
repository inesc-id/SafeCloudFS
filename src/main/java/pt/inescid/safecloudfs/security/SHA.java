package pt.inescid.safecloudfs.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SHA {

	public static boolean verifyHash(byte[] payload, byte[] hash) {
		return Arrays.equals(hash, calculateHash(payload));
	}

	public static byte[] calculateHash(byte[] input) {
		if(input == null) {
			return null;
		}
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-512");
		} catch (NoSuchAlgorithmException e) {

			System.err.println(e.getMessage());
			System.exit(-1);
		}
		digest.reset();
		digest.update(input);
		return digest.digest();
	}
}
