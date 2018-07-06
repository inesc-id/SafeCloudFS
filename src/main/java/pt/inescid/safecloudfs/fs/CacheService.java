package pt.inescid.safecloudfs.fs;

import static java.lang.Math.toIntExact;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import pt.inescid.safecloudfs.directoryService.DirectoryService;
import pt.inescid.safecloudfs.security.SHA;
import pt.inescid.safecloudfs.utils.SafeCloudFSUtils;

public class CacheService {

	private String cacheFolder;
	private boolean isCacheCiphered;
	private DirectoryService directoryService;

	public CacheService(String cacheFolder, DirectoryService directoryService) {
		this.cacheFolder = cacheFolder;
		this.directoryService = directoryService;
		if (this.cacheFolder.charAt(this.cacheFolder.length() - 1) != '/') {
			this.cacheFolder = this.cacheFolder + "/";
		}
	}

	public CacheService(String cacheFolder, boolean isCacheCiphered, DirectoryService directoryService) {
		this.cacheFolder = cacheFolder;
		this.isCacheCiphered = isCacheCiphered;
		this.directoryService = directoryService;
		if (this.cacheFolder.charAt(this.cacheFolder.length() - 1) != '/') {
			this.cacheFolder = this.cacheFolder + "/";
		}
	}

	public void saveToCache(long nlink, byte[] data) {
		if (isCacheCiphered) {
			writeCipheredByteArray(nlink, data);
		} else {
			writeByteArray(nlink, data);
		}
	}

	private void writeByteArray(long nlink, byte[] data) {

		try {
			FileOutputStream fos = new FileOutputStream(new File(this.cacheFolder + SafeCloudFSUtils.getFileName(nlink)));
			fos.write(data);
			fos.close();
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

	}

	private void writeCipheredByteArray(long nlink, byte[] data) {

	}

	public byte[] readFromCache(long nlink) {

		try {
			File cachedFile = new File(this.cacheFolder + SafeCloudFSUtils.getFileName(nlink));
			FileInputStream fis = new FileInputStream(cachedFile);
			byte[] cachedFileBytes = new byte[toIntExact(cachedFile.length())];
			fis.read(cachedFileBytes);
			fis.close();
			byte[] cachedHash = SHA.calculateHash(cachedFileBytes);

			// is cached file valid
			if (this.directoryService.isCachedFileValid(nlink, cachedHash)) {
				return cachedFileBytes;
//				return null;
			}

		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());

		}
		return null;
	}

	public void deleteFromCache(long nLink) {
		try {
			new File(this.cacheFolder + SafeCloudFSUtils.getFileName(nLink)).delete();
		}catch(Exception e) {
			SafeCloudFSUtils.LOGGER.warning("Tryed to removed cached file with error: " + e.getMessage());
		}

	}

}
