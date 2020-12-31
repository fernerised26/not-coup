package game.systems.web;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SecretUtil {

	/**
	 * Assumed to be used sparingly, all objects are generated on each call
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static String generateSecret() throws NoSuchAlgorithmException {
		SecureRandom rand = new SecureRandom();
		byte[] randomBytes = new byte[32];
		rand.nextBytes(randomBytes);
		MessageDigest digest = MessageDigest.getInstance("SHA3-256");
		byte[] hash = digest.digest(randomBytes);
		StringBuilder hexStrBld = new StringBuilder(2 * hash.length);
	    for (int i = 0; i < hash.length; i++) {
	        String hex = Integer.toHexString(0xff & hash[i]);
	        if(hex.length() == 1) {
	        	hexStrBld.append('0');
	        }
	        hexStrBld.append(hex);
	    }
	    return hexStrBld.toString();
	}
}
