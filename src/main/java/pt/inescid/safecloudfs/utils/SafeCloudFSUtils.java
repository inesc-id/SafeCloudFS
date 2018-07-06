package pt.inescid.safecloudfs.utils;

import java.util.logging.Logger;

import org.jclouds.blobstore.BlobStoreContext;

import pt.inescid.safecloudfs.SafeCloudFS;

public class SafeCloudFSUtils {

	public final static Logger LOGGER = Logger.getLogger(SafeCloudFS.class.getName());

	public static BlobStoreContext[] cloudContexts = null;


	public static String getKeyName(String path) {
		return "key." + path;
	}

	public static String getKeyName(long nLink) {
		return "key." + getFileName(nLink);
	}

	public static String getFileName(long nLink) {
		return "file." + nLink;
	}


	public static String getLogEntryFileName(long nLink, int version) {
		return "log." + nLink + "." + version;
	}


	public static String getLogEntryKeyName(long nLink, int version) {
		return "log.key." + nLink + "." + version;
	}

}
