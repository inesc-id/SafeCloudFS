package pt.inescid.safecloudfs.recovery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import pt.inescid.safecloudfs.SafeCloudFS;
import pt.inescid.safecloudfs.recovery.jbdiff.JBPatch;
import pt.inescid.safecloudfs.utils.SafeCloudFSProperties;
import pt.inescid.safecloudfs.utils.SafeCloudFSUtils;

public class FileRecover {

	public static void undoLogEntry(SafeCloudFSLogEntry logEntry) {

		if(logEntry == null) {
			return;
		}

		byte[] fileBytes = SafeCloudFS.cloudBroker.download(SafeCloudFSUtils.getFileName(logEntry.fileNLink));

		ArrayList<SafeCloudFSLogEntry> logEntries = SafeCloudFS.safeCloudFSLog.getLogEntriesByNLink(logEntry.fileNLink);
		logEntries.sort(new Comparator<SafeCloudFSLogEntry>() {

			@Override
			public int compare(SafeCloudFSLogEntry o1, SafeCloudFSLogEntry o2) {
				return o2.id - o1.id;
			}
		});


		for (int version = logEntries.get(0).version - 1; version >= logEntry.version; version-- ) {

			byte[] patch = SafeCloudFS.cloudBroker
					.download(SafeCloudFSUtils.getLogEntryFileName(logEntry.fileNLink, version));
			try {
				fileBytes = JBPatch.bspatch(fileBytes, patch);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		try {
			FileOutputStream fos= new FileOutputStream(SafeCloudFSProperties.mountedDir + "/" +  logEntry.filePath);
			fos.write(fileBytes);
			fos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}

}
