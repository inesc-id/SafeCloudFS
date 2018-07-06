package pt.inescid.safecloudfs.recovery;

import java.util.Calendar;

import com.google.gson.Gson;

import pt.inescid.safecloudfs.utils.SafeCloudFSProperties;

/**
 *
 *
 * From de article: "The metadata that is logged in the coordination service for
 * an operation, lmf u , contains a timestamp, the user identifier, the file
 * name, the version identifier and the operation (create, update or delete)."
 *
 *
 * @author davidmatos
 *
 */
public class SafeCloudFSLogEntry {

	public int id;
	public long timestamp;
	public long userId;
	public String filePath;
	public long fileNLink;
	public int version;
	public SafeCloudFSOperation operation;
	public String clientIpAddress;
	public long mode;

	public SafeCloudFSLogEntry() {
		this.timestamp = Calendar.getInstance().getTimeInMillis();
		this.clientIpAddress = SafeCloudFSProperties.CLIENT_IP_ADDRESS;
	}

	public SafeCloudFSLogEntry(int id, long userId, String filePath, long fileNLink, SafeCloudFSOperation operation,
			long mode) {
		this.timestamp = Calendar.getInstance().getTimeInMillis();
		this.userId = userId;
		this.filePath = filePath;
		this.fileNLink = fileNLink;
		this.version = 0;
		this.operation = operation;
		this.mode = mode;
		this.clientIpAddress = SafeCloudFSProperties.CLIENT_IP_ADDRESS;
	}

	public SafeCloudFSLogEntry(int id, long userId, String filePath, long fileNLink, int version,
			SafeCloudFSOperation operation, long mode) {
		this.timestamp = Calendar.getInstance().getTimeInMillis();
		this.userId = userId;
		this.filePath = filePath;
		this.fileNLink = fileNLink;
		this.version = version;
		this.operation = operation;
		this.mode = mode;
		this.clientIpAddress = SafeCloudFSProperties.CLIENT_IP_ADDRESS;
	}

	@Override
	public String toString() {

		Gson gson = new Gson();
		return gson.toJson(this);

	}

	@Override
	public boolean equals(Object obj) {

			SafeCloudFSLogEntry entry = (SafeCloudFSLogEntry) obj;

			return entry.clientIpAddress.equals(this.clientIpAddress) && entry.fileNLink == this.fileNLink
					&& entry.filePath.equals(this.filePath) && entry.mode == this.mode
					&& entry.operation == this.operation // && entry.timestamp == this.timestamp
					&& entry.userId == this.userId && entry.version == this.version && entry.id == this.id;

	}

	@Override
	public int hashCode() {

		String result = id + "#" + userId + "#" + filePath + "#" + fileNLink + "#" + version + "#" + operation
				+ "#" + clientIpAddress + "#" + mode;


		return result.hashCode();
	}

}
