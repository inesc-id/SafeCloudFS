package pt.inescid.safecloudfs.directoryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * In memory directory service to avoid using DepSpace or Zookeeper for simple
 * tests
 *
 * @author davidmatos
 *
 */

public class LocalDirectoryService implements DirectoryService {

	private long currentNLink = 2;

	private static final String ST_NLINK = "st_nlink";
	private static final String ST_MODE = "st_mode";
	private static final String IS_DIR = "is_dir";
	// private static final String CONTENT = "content";

	private HashMap<String, HashMap<String, String>> directory;

	public LocalDirectoryService() {
		directory = new HashMap<String, HashMap<String, String>>();

		HashMap<String, String> rootNode = new HashMap<>();
		rootNode.put(IS_DIR, new Boolean(true).toString());
		rootNode.put(ST_NLINK, "1");

		this.directory.put("/", rootNode);

	}

	public void mv(String originPath, String destinationPath) {
		synchronized (this) {
			directory.put(destinationPath, directory.get(originPath));
			directory.remove(originPath);
		}

	}

	public void cp(String originPath, String destinationPath) {
		directory.put(destinationPath, directory.get(originPath));

	}

	public void rm(String path) {
		directory.remove(path);

	}

	public long getNLink(String path) {
		if (this.directory.containsKey(path)) {
			if (this.directory.get(path).get(ST_NLINK) != null) {
				long result = Long.parseLong(this.directory.get(path).get(ST_NLINK));
				return result;
			}
		}
		return -1;

	}

	public Number getMode(String path) {

		return Integer.parseInt(this.directory.get(path).get(ST_MODE));
	}

	public void setMode(String path, Number mode) {
		HashMap<String, String> node = this.directory.get(path);
		node.put(ST_MODE, mode + "");
		this.directory.put(path, node);
	}

	@Override
	public String toString() {
		Set<String> keys = this.directory.keySet();
		String result = "";
		for (String path : keys) {
			result += path + "\n";
		}
		return result;
	}

	public boolean exists(String path) {

		return this.directory.containsKey(path);
	}

	@Override
	public boolean isDir(String path) {
		if (path.equals("/")) {
			return true;
		}
		return Boolean.parseBoolean(this.directory.get(path).get(IS_DIR));

	}

	@Override
	public String getParent(String path) {
		if (path.equals("/")) {
			return null;
		}

		String parentPath = "/";
		if (path.lastIndexOf("/") > 0) {
			parentPath = path.substring(0, path.lastIndexOf("/"));
		}

		if (this.directory.containsKey(parentPath)) {
			return parentPath;
		} else {
			return null;
		}
	}

	@Override
	public void rmdir(String path) {
		Set<String> keySet = this.directory.keySet();
		for (String p : keySet) {
			if (p.startsWith(path)) {
				this.directory.remove(p);
			}
		}
	}

	@Override
	public void mkfile(String path) {
		HashMap<String, String> node = new HashMap<String, String>();
		node.put(IS_DIR, new Boolean(false).toString());
		node.put(ST_NLINK, this.currentNLink + "");
		// node.put(ST_MODE, mode+"");
		this.currentNLink++;

		this.directory.put(path, node);

	}

	@Override
	public void mkdir(String path) {
		HashMap<String, String> node = new HashMap<String, String>();
		node.put(IS_DIR, new Boolean(true).toString());
		this.directory.put(path, node);
	}

	@Override
	public ArrayList<String> readDir(String path) {

		ArrayList<String> result = new ArrayList<>();
		int pathDashes = getNrOfDashesFromString(path);
		try {

			Set<String> entries = this.directory.keySet();
			for (String entry : entries) {
				if(entry.equals(path) || !entry.startsWith(path)) {
					continue;
				}





				int entryDashes = getNrOfDashesFromString(entry);



				if(pathDashes != (entryDashes -1) && !path.equals("/")) {
					continue;
				}

				if(pathDashes != entryDashes && path.equals("/")) {
					continue;
				}


				result.add(entry.substring(entry.lastIndexOf('/') +1 ));


			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	private int getNrOfDashesFromString(String path) {
		int result = 0;
		for(int i = 0; i < path.length(); i++) {
			if(path.charAt(i) == '/') {
				result++;
			}
		}
		return result;
	}

	@Override
	public boolean isCachedFileValid(long nLink, byte[] hash) {

		return true;
	}

}
