package pt.inescid.safecloudfs.recovery;

import java.util.ArrayList;
import java.util.HashSet;

import pt.inescid.safecloudfs.directoryService.DirectoryService;
import pt.inescid.safecloudfs.recovery.gui.SafeCloudFSLogViewer;

/**
 *
 * @author davidmatos
 *
 */
public class SafeCloudFSLogLocal implements SafeCloudFSLog {

	private int currentId = 1;

	private HashSet<SafeCloudFSLogEntry> entries;
	private SafeCloudFSLogViewer window;

	public SafeCloudFSLogLocal(SafeCloudFSLogViewer window, DirectoryService directoryService) {
		this.entries = new HashSet<>();
		this.window = window;

	}

	@Override
	public int log(SafeCloudFSLogEntry safeCloudFsLogEntry) {
		int currentVersion = 1;
		try {

			synchronized (this) {
				for (SafeCloudFSLogEntry entry : entries) {
					if (entry.fileNLink == safeCloudFsLogEntry.fileNLink && entry.operation == SafeCloudFSOperation.WRITE) {
						if (entry.version >= currentVersion) {
							currentVersion = entry.version+1;
						}
					}
				}
			}


			safeCloudFsLogEntry.version = currentVersion;

			for (SafeCloudFSLogEntry entry : this.entries) {
				if (entry.equals(safeCloudFsLogEntry)) {
					return safeCloudFsLogEntry.version;
				}
			}

			synchronized (this) {
				safeCloudFsLogEntry.id = this.currentId;
				this.currentId++;
				this.entries.add(safeCloudFsLogEntry);
			}

			if(window != null) {
				window.addLogEntry(safeCloudFsLogEntry);
			}


		} catch (Exception e) {
			e.printStackTrace();
		}
		return currentVersion;
	}

	@Override
	public SafeCloudFSLogEntry getLogEntryById(int id) {
		for(SafeCloudFSLogEntry entry : entries) {
			if(entry.id == id) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public HashSet<SafeCloudFSLogEntry> getLogEntries() {



		return this.entries;
	}

	@Override
	public String toString() {
		String result = "";
		for (SafeCloudFSLogEntry entry : this.entries) {
			result += entry.toString() + "\n";
		}
		return result;
	}

	@Override
	public ArrayList<SafeCloudFSLogEntry> getLogEntriesByPath(String path) {
		ArrayList<SafeCloudFSLogEntry> result = new ArrayList<>();
		for (SafeCloudFSLogEntry entry : this.entries) {
			if (entry.filePath.equals(path)) {
				result.add(entry);
			}
		}
		return result;
	}

	@Override
	public ArrayList<SafeCloudFSLogEntry> getLogEntriesByNLink(long nLink) {
		ArrayList<SafeCloudFSLogEntry> result = new ArrayList<>();
		for (SafeCloudFSLogEntry entry : this.entries) {
			if (entry.fileNLink == nLink) {
				result.add(entry);
			}
		}
		return result;
	}

}
