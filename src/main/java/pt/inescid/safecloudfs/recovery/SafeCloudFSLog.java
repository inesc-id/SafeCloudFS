package pt.inescid.safecloudfs.recovery;

import java.util.ArrayList;
import java.util.HashSet;

public interface SafeCloudFSLog {



	public int log(SafeCloudFSLogEntry safeCloudFsLogEntry);

	public SafeCloudFSLogEntry getLogEntryById(int id);

	public ArrayList<SafeCloudFSLogEntry> getLogEntriesByPath(String path);

	public ArrayList<SafeCloudFSLogEntry> getLogEntriesByNLink(long nLink);

	public HashSet<SafeCloudFSLogEntry> getLogEntries();


}
