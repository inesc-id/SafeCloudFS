package pt.inescid.safecloudfs;

import java.awt.EventQueue;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;

import com.google.devtools.common.options.OptionsParser;

import pt.inescid.safecloudfs.cloud.CloudBroker;
import pt.inescid.safecloudfs.cloud.CloudUtils;
import pt.inescid.safecloudfs.directoryService.DepSpaceClient;
import pt.inescid.safecloudfs.directoryService.DirectoryService;
import pt.inescid.safecloudfs.directoryService.LocalDirectoryService;
import pt.inescid.safecloudfs.directoryService.ZookeeperClient;
import pt.inescid.safecloudfs.fs.CacheService;
import pt.inescid.safecloudfs.fs.SafeCloudFileSystem;
import pt.inescid.safecloudfs.recovery.SafeCloudFSLog;
import pt.inescid.safecloudfs.recovery.SafeCloudFSLogDepSpace;
import pt.inescid.safecloudfs.recovery.SafeCloudFSLogLocal;
import pt.inescid.safecloudfs.recovery.SafeCloudFSLogZookeeper;
import pt.inescid.safecloudfs.recovery.gui.SafeCloudFSLogViewer;
import pt.inescid.safecloudfs.utils.SafeCloudFSOptions;
import pt.inescid.safecloudfs.utils.SafeCloudFSProperties;
import pt.inescid.safecloudfs.utils.SafeCloudFSUtils;

public class SafeCloudFS {

	public static CloudBroker cloudBroker;

	public static SafeCloudFSLog safeCloudFSLog;

	public static DirectoryService directoryService;

	public static void main(String[] args) {

		// Verify the args given by the user in the command line
		SafeCloudFSOptions options = verifyArgs(args);

		Level logLevel = options.debug.isEmpty() ? Level.WARNING : Level.parse(options.debug);
		SafeCloudFSUtils.LOGGER.setLevel(logLevel);

		SafeCloudFSProperties.mountedDir = options.mountDirectory;

		if(!new File(SafeCloudFSProperties.mountedDir).exists()) {
			new File(SafeCloudFSProperties.mountedDir).mkdirs();
		}

		try {
			SafeCloudFSProperties.clientIpAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			System.err.println("Couldn't get client IP address.\n" + e1.getMessage());
			System.exit(-1);
		}

		SafeCloudFSProperties.loadPropertiesFile(options.configFile);



		if (SafeCloudFSProperties.zookeeperHost != null) {
			directoryService = new ZookeeperClient();
			safeCloudFSLog = new SafeCloudFSLogZookeeper();
		} else if (SafeCloudFSProperties.depspaceHostsFile != null) {
			directoryService = new DepSpaceClient(SafeCloudFSProperties.depspaceHostsFile);
			safeCloudFSLog = new SafeCloudFSLogDepSpace();
		} else {
			directoryService = new LocalDirectoryService();
		}

		CacheService cacheService = null;
		if (SafeCloudFSProperties.useCache) {


			if (!new File(SafeCloudFSProperties.cacheDir).exists()) {
				new File(SafeCloudFSProperties.cacheDir).mkdirs();
			} else {
				File cacheFolder = new File(SafeCloudFSProperties.cacheDir);
				if (!cacheFolder.isDirectory()) {
					System.err.println("Cache path is not a dir");
					System.exit(-1);
				}
				File[] cacheFiles = cacheFolder.listFiles();
				for (int i = 0; i < cacheFiles.length; i++) {
					cacheFiles[i].delete();
				}
			}

			boolean isCacheCiphered = false;
			cacheService = new CacheService(SafeCloudFSProperties.cacheDir, isCacheCiphered, directoryService);

		}

		SafeCloudFSUtils.LOGGER.info("SafeCloudFS will ping clouds to check if they are accessible");
		cloudBroker = CloudUtils.pingClouds();
		SafeCloudFSUtils.LOGGER.info("All clouds are accessible.");

		SafeCloudFSUtils.LOGGER.log(Level.INFO, "Started SafeCloudFS - Params: " + Arrays.asList(args).toString());

		SafeCloudFileSystem safeCloudFileSystem = new SafeCloudFileSystem(SafeCloudFSProperties.mountedDir,
				directoryService, cloudBroker, safeCloudFSLog, cacheService);

		// MemoryFS fuse= new MemoryFS();

		try {

			// fuse.mount(Paths.get(RockFSConfig.MOUNTED_DIR), true, true);

			safeCloudFileSystem.mount(true, true);

			if (SafeCloudFSProperties.recoveryGui) {
				SafeCloudFSLogViewer window = new SafeCloudFSLogViewer(directoryService, null);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {

							window.frame.setVisible(true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				safeCloudFSLog = new SafeCloudFSLogLocal(window, directoryService);
				window.setLog(safeCloudFSLog);
			}else {
				safeCloudFSLog = new SafeCloudFSLogLocal(null, directoryService);
			}


		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			safeCloudFileSystem.umount();
			// fuse.umount();
		}
	}

	/**
	 * Verifies the arguments given by the users
	 *
	 * @param args
	 * @return
	 */
	private static SafeCloudFSOptions verifyArgs(String[] args) {
		OptionsParser parser = OptionsParser.newOptionsParser(SafeCloudFSOptions.class);
		parser.parseAndExitUponError(args);
		SafeCloudFSOptions options = parser.getOptions(SafeCloudFSOptions.class);
		if (options.configFile.isEmpty() || options.mountDirectory.isEmpty()) {
			printUsage(parser);
			System.exit(-1);
		}
		return options;
	}

	/**
	 * Prints the command line arguments
	 *
	 * @param parser
	 */
	private static void printUsage(OptionsParser parser) {
		System.out.println("Usage: java -jar server.jar OPTIONS");
		System.out.println(
				parser.describeOptions(Collections.<String, String>emptyMap(), OptionsParser.HelpVerbosity.LONG));
	}

}
