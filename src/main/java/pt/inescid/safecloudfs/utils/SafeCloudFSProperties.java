package pt.inescid.safecloudfs.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class SafeCloudFSProperties {

	//Constants
	public static final boolean SYNC = true;
	public static final boolean ASYNC = false;

	public static final boolean DEPSKY_A = true;
	public static final boolean DEPSKY_CA = false;

	private static final String PROPERTIES_FILE_SYSTEM_PROTOCOL = "filesystem.protocol";
	private static final String PROPERTIES_UPLOAD_METHOD = "upload.method";
	private static final String PROPERTIES_CLOUDS_F = "clouds.f";
	private static final String PROPERTIES_DEPSPACE_HOSTS_FILE = "depspace.hosts.file";
	private static final String PROPERTIES_ACCESS_KEYS_FILE = "access.keys.file";
	private static final String PROPERTIES_CACHE_DIR = "cache.dir";
	private static final String PROPERTIES_RECOVERY_GUI = "recovery.gui";
	private static final String PROPERTIES_ZOOKEEPER_HOST = "zookeeper.host";




	//Execution properties of the file system
	public static String mountedDir = "";
	public static boolean fileSystemProtocol = DEPSKY_A;
	public static String cloudAccountsFile = "";
	public static boolean uploadMethod = SYNC;
	public static int cloudsF = 0;
	public static int cloudsN = 0;
	public static String depspaceHostsFile;
	public static boolean useCache = false;
	public static String cacheDir = "";
	public static String clientIpAddress;
	public static boolean recoveryGui;
	public static String zookeeperHost;



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

			fileSystemProtocol = properties.getProperty(PROPERTIES_FILE_SYSTEM_PROTOCOL).equalsIgnoreCase("DepSky-A");
			uploadMethod = properties.getProperty(PROPERTIES_UPLOAD_METHOD).equals("sync");
			cloudsF = Integer.parseInt(properties.getProperty(PROPERTIES_CLOUDS_F));
			depspaceHostsFile = properties.getProperty(PROPERTIES_DEPSPACE_HOSTS_FILE);
			cloudAccountsFile = properties.getProperty(PROPERTIES_ACCESS_KEYS_FILE);
			cacheDir = properties.getProperty(PROPERTIES_CACHE_DIR);
			useCache  = (cacheDir != null);
			recoveryGui = properties.getProperty(PROPERTIES_RECOVERY_GUI).equalsIgnoreCase("true");
			zookeeperHost = properties.getProperty(PROPERTIES_ZOOKEEPER_HOST);



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
