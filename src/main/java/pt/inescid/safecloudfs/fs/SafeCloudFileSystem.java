package pt.inescid.safecloudfs.fs;

import static java.lang.Math.toIntExact;
import static jnr.ffi.Platform.OS.WINDOWS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import jnr.ffi.Platform;
import jnr.ffi.Platform.OS;
import jnr.ffi.Pointer;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import pt.inescid.safecloudfs.cloud.CloudBroker;
import pt.inescid.safecloudfs.directoryService.DirectoryService;
import pt.inescid.safecloudfs.recovery.SafeCloudFSLog;
import pt.inescid.safecloudfs.recovery.SafeCloudFSLogEntry;
import pt.inescid.safecloudfs.recovery.SafeCloudFSOperation;
import pt.inescid.safecloudfs.recovery.jbdiff.JBDiff;
import pt.inescid.safecloudfs.utils.SafeCloudFSProperties;
import pt.inescid.safecloudfs.utils.SafeCloudFSUtils;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import ru.serce.jnrfuse.struct.Statvfs;

public class SafeCloudFileSystem extends FuseStubFS {

	private String mountFolder;

	private DirectoryService directoryService;

	private HashMap<String, ByteBuffer> contents;

	private SafeCloudFSLog log;

	private CloudBroker cloudBroker;

	private CacheService cacheService;

	public SafeCloudFileSystem(String mountFolder, DirectoryService directoryService, CloudBroker cloudUploader,
			SafeCloudFSLog log) {
		super();
		this.cloudBroker = cloudUploader;
		this.contents = new HashMap<>();
		this.mountFolder = mountFolder;
		this.directoryService = directoryService;
		this.log = log;

	}

	public SafeCloudFileSystem(String mountFolder, DirectoryService directoryService, CloudBroker cloudUploader,
			SafeCloudFSLog log, CacheService cacheService) {
		super();
		this.cloudBroker = cloudUploader;
		this.contents = new HashMap<>();
		this.mountFolder = mountFolder;
		this.directoryService = directoryService;
		this.log = log;
		this.cacheService = cacheService;
	}

	public void mount(boolean blocking, boolean debug) {
		try {
			SafeCloudFSUtils.LOGGER.log(Level.INFO, "OPERATION: mount");
			Path path = Paths.get(this.mountFolder);

			jnr.ffi.Platform p = jnr.ffi.Platform.getNativePlatform();

			switch (p.getOS()) {
			case LINUX:
				mount(path, blocking, debug);
				break;
			case DARWIN:
				mount(path, blocking, debug, new String[] { "-o", "volname=SafeCloudFS", "-o", "fsname=SafeCloudFS",
						"-o", "blocksize=4096", "-o", "allow_other" });
				break;
			case WINDOWS:
				mount(path, blocking, debug);
				break;
			default:

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public int create(String path, @mode_t long mode, FuseFileInfo fi) {
		try {

			System.out.println(fi.toString());
			System.out.println("Create: path=" + path + " mode=" + mode);
			if (this.directoryService.exists(path)) {
				return -ErrorCodes.EEXIST();
			}

			String parentPath = this.directoryService.getParent(path);

			if (this.directoryService.isDir(parentPath)) {

				this.directoryService.mkfile(path);

				this.contents.put(path, ByteBuffer.allocate(0));

				SafeCloudFSLogEntry logEntry = new SafeCloudFSLogEntry(0, getContext().uid.get(), path,
						this.directoryService.getNLink(path), 0, SafeCloudFSOperation.CREATE, mode);
				this.log.log(logEntry);

				return 0;
			}
			return -ErrorCodes.ENOENT();
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	@Override
	public int getattr(String path, FileStat stat) {
		try {
			if (this.directoryService.exists(path)) {

				if (this.directoryService.isDir(path)) {
					stat.st_mode.set(FileStat.S_IFDIR | 0777);
				} else {
					stat.st_mode.set(FileStat.S_IFREG | 0777);
					stat.st_size.set(contents.get(path).capacity());
				}

				stat.st_uid.set(getContext().uid.get());
				stat.st_gid.set(getContext().gid.get());



				return 0;
			}
			return -ErrorCodes.ENOENT();
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	@Override
	public int mkdir(String path, @mode_t long mode) {
		try {
			if (this.directoryService.exists(path)) {
				return -ErrorCodes.EEXIST();
			}
			String parentPath = this.directoryService.getParent(path);
			if (this.directoryService.isDir(parentPath)) {
				this.directoryService.mkdir(path);

				SafeCloudFSLogEntry logEntry = new SafeCloudFSLogEntry(0, getContext().uid.get(), path,
						this.directoryService.getNLink(path), 0, SafeCloudFSOperation.MKDIR, mode);
				this.log.log(logEntry);

				SafeCloudFSUtils.LOGGER.info("Directory " + path + " was created.");

				return 0;
			}
			return -ErrorCodes.ENOENT();
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	@Override
	public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
		try {

			if (!this.directoryService.exists(path)) {
				return -ErrorCodes.ENOENT();
			}
			if (this.directoryService.isDir(path)) {
				return -ErrorCodes.EISDIR();
			}
			return readFile(path, buf, size, offset);
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	private int readFile(String path, Pointer buffer, long size, long offset) {
		long nLink = this.directoryService.getNLink(path);

		if (this.cacheService != null) {
			byte[] bytes = this.cacheService.readFromCache(this.directoryService.getNLink(path));
			if (bytes != null) {
				SafeCloudFSUtils.LOGGER.info("Fetching file " + path + " from local cache.");
				buffer.put(0, bytes, 0, bytes.length);
				return bytes.length;
			}
		}
		// If the file is not in the cache, then I must read it from the cloud
		SafeCloudFSUtils.LOGGER.info("Fetching file " + path + "(" + path + ")" + " from cloud(s).");
		byte[] bytes = this.cloudBroker.download(SafeCloudFSUtils.getFileName(nLink));
		buffer.put(0, bytes, 0, bytes.length);
		return bytes.length;

		// int bytesToRead = (int) Math.min(contents.get(path).capacity() - offset,
		// size);
		// byte[] bytesRead = new byte[bytesToRead];
		// synchronized (this) {
		// contents.get(path).position((int) offset);
		// contents.get(path).get(bytesRead, 0, bytesToRead);
		// buffer.put(0, bytesRead, 0, bytesToRead);
		// contents.get(path).position(0); // Rewind
		// }
		// return bytesToRead;
	}

	@Override
	public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {
		try {
			if (!this.directoryService.exists(path)) {
				return -ErrorCodes.ENOENT();
			}
			if (!this.directoryService.isDir(path)) {
				return -ErrorCodes.ENOTDIR();
			}
			filter.apply(buf, ".", null, 0);
			filter.apply(buf, "..", null, 0);

			ArrayList<String> dirContent = this.directoryService.readDir(path);
			for (String nodeName : dirContent) {

				filter.apply(buf, nodeName, null, 0);
			}

			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	@Override
	public int statfs(String path, Statvfs stbuf) {
		try {
			if (Platform.getNativePlatform().getOS() == WINDOWS) {
				if ("/".equals(path)) {
					stbuf.f_blocks.set(1024 * 1024); // total data blocks in file system
					stbuf.f_frsize.set(1024); // fs block size
					stbuf.f_bfree.set(1024 * 1024); // free blocks in fs
				}
			}
			if (Platform.getNativePlatform().getOS() == OS.DARWIN) {
				if ("/".equals(path)) {

					stbuf.f_frsize.set(4096); /* fragment size */
					stbuf.f_blocks.set(1024 * 1024); /* size of fs in f_frsize units */
					stbuf.f_bfree.set(1024 * 1024); /* # free blocks */
					stbuf.f_bavail.set(1024 * 1024); /* # free blocks for non-root */

				}
			}

			return super.statfs(path, stbuf);
		} catch (Exception e) {
			e.printStackTrace();
			return super.statfs(path, stbuf);
		}
	}

	@Override
	public int rename(String path, String newName) {
		try {

			if (!this.directoryService.exists(path)) {
				return -ErrorCodes.ENOENT();
			}
			String newParent = this.directoryService.getParent(newName);
			if (!this.directoryService.exists(newParent)) {
				return -ErrorCodes.ENOENT();
			}

			if (!this.directoryService.isDir(newParent)) {
				return -ErrorCodes.ENOTDIR();
			}
			this.directoryService.mv(path, newName);

			SafeCloudFSLogEntry logEntry = new SafeCloudFSLogEntry(0, getContext().uid.get(), path,
					this.directoryService.getNLink(path), 0, SafeCloudFSOperation.RENAME, 0);
			this.log.log(logEntry);

			SafeCloudFSUtils.LOGGER.info("File " + path + " was renamed to " + newName);
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	@Override
	public int rmdir(String path) {
		try {
			if (!this.directoryService.exists(path)) {
				return -ErrorCodes.ENOENT();
			}
			if (!this.directoryService.isDir(path)) {
				return -ErrorCodes.ENOTDIR();
			}
			this.directoryService.rmdir(path);

			SafeCloudFSLogEntry logEntry = new SafeCloudFSLogEntry(0, getContext().uid.get(), path,
					this.directoryService.getNLink(path), 0, SafeCloudFSOperation.RMDIR, 0);
			this.log.log(logEntry);
			SafeCloudFSUtils.LOGGER.info("Directory " + path + " was removed.");

			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	@Override
	public int truncate(String path, long offset) {
		System.out.println("truncate");
		try {
			if (!this.directoryService.exists(path)) {
				return -ErrorCodes.ENOENT();
			}
			if (this.directoryService.isDir(path)) {
				return -ErrorCodes.EISDIR();
			}
			truncateFile(path, offset);
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	private synchronized void truncateFile(String path, long size) {
		if (size < contents.get(path).capacity()) {
			// Need to create a new, smaller buffer
			ByteBuffer newContents = ByteBuffer.allocate((int) size);
			byte[] bytesRead = new byte[(int) size];
			contents.get(path).get(bytesRead);
			newContents.put(bytesRead);
			contents.put(path, newContents);
		}
	}

	@Override
	public int unlink(String path) {
		try {

			System.out.println("unlink");
			if (!this.directoryService.exists(path)) {
				return -ErrorCodes.ENOENT();
			}

			this.cloudBroker.remove(this.directoryService.getNLink(path) + "");
			this.directoryService.rm(path);

			long nLink = this.directoryService.getNLink(path);

			SafeCloudFSLogEntry logEntry = new SafeCloudFSLogEntry(0, getContext().uid.get(), path, nLink, 0,
					SafeCloudFSOperation.UNLINK, 0);
			this.log.log(logEntry);

			if (this.cacheService != null) {
				this.cacheService.deleteFromCache(nLink);
			}

			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	@Override
	public int open(String path, FuseFileInfo fi) {
		try {
			System.out.println("open");

			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
		try {
			System.out.println("write");
			if (!this.directoryService.exists(path)) {
				return -ErrorCodes.ENOENT();
			}
			if (this.directoryService.isDir(path)) {
				return -ErrorCodes.EISDIR();
			}

			long nLink = this.directoryService.getNLink(path);
			SafeCloudFSLogEntry logEntry = new SafeCloudFSLogEntry(0, getContext().uid.get(), path, nLink, 0,
					SafeCloudFSOperation.WRITE, 0);
			int version = this.log.log(logEntry);


			logDelta(path, buf, size, offset, fi, version-1
					//because the version given by this.log.log corresponds to the CURRENT file, not the previous one
					);

			return write(path, buf, size, offset, nLink);
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	// Send to the clouds
	private int write(String path, Pointer buffer, long bufSize, long writeOffset, long nLink) {

		if (!this.contents.containsKey(path)) {
			this.contents.put(path, ByteBuffer.allocate(toIntExact(bufSize)));
		}
		if (this.contents.get(path).capacity() == 0) {
			this.contents.put(path, ByteBuffer.allocate(toIntExact(bufSize)));
		}

		byte[] bytes = new byte[toIntExact(bufSize)];
		for (long i = 0; i < bufSize; i++) {
			bytes[toIntExact(i)] = buffer.getByte(i);
		}

		// System.out.println("path=" + path + " content=" + new String(bytes) + "
		// bufSize=" + bufSize);
		cloudBroker.upload(SafeCloudFSUtils.getFileName(nLink), bytes);

		if (this.cacheService != null) {
			this.cacheService.saveToCache(nLink, bytes);
		}

		int maxWriteIndex = (int) (writeOffset + bufSize);
		byte[] bytesToWrite = new byte[(int) bufSize];

		synchronized (this) {
			if (maxWriteIndex > this.contents.get(path).capacity()) {
				// Need to create a new, larger buffer
				ByteBuffer newContents = ByteBuffer.allocate(maxWriteIndex);
				newContents.put(contents.get(path));
				contents.put(path, newContents);
			}
			buffer.get(0, bytesToWrite, 0, (int) bufSize);

			contents.get(path).position((int) writeOffset);
			contents.get(path).put(bytesToWrite);
			contents.get(path).position(0); // Rewind

		}

		System.out.println("Le size: " + bufSize + " le offesset:" + writeOffset);
		return toIntExact(bufSize);
	}


	/**
	 * Logs the data part of the file to the cloud providers
	 * It only uploads the differences between the previous and the current versions
	 * This way it saves some storage
	 * @param path
	 * @param buffer
	 * @param size
	 * @param offset
	 * @param fi
	 * @param versionNumber
	 */
	private void logDelta(String path, Pointer buffer, long size, long offset, FuseFileInfo fi, int versionNumber) {
		long nLink = this.directoryService.getNLink(path);

		System.out.println("Will log version " + versionNumber);
		if (versionNumber == 0) {
			//There is no delta o calculate since this is the first version of the file
			return;
		}

		// first we get the previous version
		byte[] previousVersion = null;
		if (this.cacheService != null) {
			previousVersion = this.cacheService.readFromCache(nLink);
			if (previousVersion != null) {
				SafeCloudFSUtils.LOGGER.info("Fetching file " + path + " from local cache.");
				buffer.put(0, previousVersion, 0, previousVersion.length);
			}
		} else {
			SafeCloudFSUtils.LOGGER.info(
					"Fetching file " + path + "(" + this.directoryService.getNLink(path) + ")" + " from cloud(s).");
			previousVersion = this.cloudBroker.download(SafeCloudFSUtils.getFileName(nLink));
			buffer.put(0, previousVersion, 0, previousVersion.length);
		}

		// first, we move

		byte[] currentVersion = new byte[toIntExact(size)];
		for (long i = 0; i < size; i++) {
			currentVersion[toIntExact(i)] = buffer.getByte(i);
		}

		try {
			byte[] delta = JBDiff.bsdiff(currentVersion, previousVersion);

			cloudBroker.upload(SafeCloudFSUtils.getLogEntryFileName(nLink, versionNumber), delta);

			// if we are running CA protocol then we must also backup the previous version
			// key file
			if (SafeCloudFSProperties.FILESYSTEM_PROTOCOL == SafeCloudFSProperties.DEPSKY_CA
					&& SafeCloudFSProperties.CLOUDS_N > 1) {
				byte[] key = cloudBroker.download(SafeCloudFSUtils.getKeyName(nLink));
				cloudBroker.upload(SafeCloudFSUtils.getLogEntryKeyName(nLink, versionNumber), key);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public int chmod(String path, long mode) {
		try {
			System.out.println("CHMOD  " + mode);
			this.directoryService.setMode(path, mode);

			SafeCloudFSLogEntry logEntry = new SafeCloudFSLogEntry(0, getContext().uid.get(), path,
					this.directoryService.getNLink(path), SafeCloudFSOperation.CHMOD, mode);
			this.log.log(logEntry);

			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;

		}
	}

	@Override
	public int setxattr(String path, String name, Pointer value, long size, int flags) {
		// TODO Auto-generated method stub

		// System.out.println("XATTR=" + name + " value=" + value.toString());

		return super.setxattr(path, name, value, size, flags);
	}

}
