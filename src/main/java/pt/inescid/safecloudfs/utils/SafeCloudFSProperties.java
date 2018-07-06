package pt.inescid.safecloudfs.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class SafeCloudFSProperties {


	public static final boolean SYNC = true;
	public static final boolean ASYNC = false;

	public static final boolean DEPSKY_A = true;
	public static final boolean DEPSKY_CA = false;

	public static final String PROPERTIES_FILE_SYSTEM_PROTOCOL = "filesystem.protocol";
	public static final String PROPERTIES_UPLOAD_METHOD = "upload.method";
	public static final String PROPERTIES_CLOUDS_F = "clouds.f";

	public static String MOUNTED_DIR = "";

	public static boolean FILESYSTEM_PROTOCOL = DEPSKY_A;

	public static String ACCOUNTS_FILE = "";

	public static int NUMBER_OF_CLOUDS = 0;

	public static boolean UPLOAD_METHOD = SYNC;

	public static int CLOUDS_F = 0;
	public static int CLOUDS_N = 0;

	public static String DEPSPACE_HOSTS_FILE;

	public static boolean USE_CACHE = false;
	public static String CACHE_DIR = "";

	public static String CLIENT_IP_ADDRESS;


	/**
	 * loads the configuration file given by the user to RockFSConfig
	 *
	 * @param configFile
	 */
	public static void loadPropertiesFile(String configFile) {
		FileInputStream fis = null;
		try {

			fis = new FileInputStream(new File(configFile));
			Properties properties = new Properties();

			properties.load(fis);

			FILESYSTEM_PROTOCOL = properties.getProperty(PROPERTIES_FILE_SYSTEM_PROTOCOL).equalsIgnoreCase("DepSky-A");

			UPLOAD_METHOD = properties.getProperty(PROPERTIES_UPLOAD_METHOD).equals("sync");
			CLOUDS_F = Integer.parseInt(properties.getProperty(PROPERTIES_CLOUDS_F));

		} catch (IOException e) {
			System.err.println("Couldn't open config file in " + configFile);
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (NumberFormatException nfe) {
			System.err.println(nfe.getMessage());
			System.exit(-1);
		}

	}

}
