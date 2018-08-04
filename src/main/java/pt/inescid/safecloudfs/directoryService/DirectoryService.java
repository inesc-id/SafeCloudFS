package pt.inescid.safecloudfs.directoryService;

import java.util.ArrayList;

/**
 * Abstraction to the coordination service, whether it is Zookeeper or DepSpace
 * @author davidmatos
 *
 */
public interface DirectoryService {

	public void init(long uid, long gid) ;

	public void mv(String originPath, String destinationPath);

	public void rm(String path);

	public void rmdir(String path);

	public void mkfile(String path, long uid, long gid);

	public void mkdir(String path, long uid, long gid);

	public long getNLink(String path);

	public Number getMode(String path);

	public void setMode(String path, Number mode);

	public boolean exists(String path);

	public boolean isDir(String path);

	public String getParent(String path);

	public ArrayList<String> readDir(String path);

	public boolean isCachedFileValid(long nLink, byte[] hash);

	public int chown(String path, long uid, long gid);

	public long getUid(String path);

	public long getGid(String path);
}
