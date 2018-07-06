package pt.inescid.safecloudfs.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CloudAccounts {

	private static CloudAccount[] accounts = null;

	public static CloudAccount getAccount(int id) {
		if (getAccounts().length <= id) {
			SafeCloudFSUtils.LOGGER.severe("There are only " + getAccounts().length);
			System.exit(-1);
		}
		return getAccounts()[id];
	}

	public static CloudAccount[] getAllAccounts() {
		return getAccounts();
	}

	public static int getAccountsSize() {
		return getAccounts().length;
	}

	private  static CloudAccount[] getAccounts() {
		if (accounts != null) {
			return accounts;
		}
		try {
			Reader reader = new InputStreamReader(new FileInputStream(new File(SafeCloudFSProperties.ACCOUNTS_FILE)), "UTF-8");

			Gson gson = new GsonBuilder().create();

			CloudAccount[] accounts = gson.fromJson(reader, CloudAccount[].class);
			SafeCloudFSProperties.CLOUDS_N = accounts.length;
			return accounts;

		} catch (FileNotFoundException fnfe) {

			SafeCloudFSUtils.LOGGER.severe("Cloud accounts file with access credentials does not exist in: '"
					+ SafeCloudFSProperties.ACCOUNTS_FILE + "'");
			System.exit(-1);
		} catch (UnsupportedEncodingException e) {
			SafeCloudFSUtils.LOGGER
					.severe("Unsupported encoding of file: '" + SafeCloudFSProperties.ACCOUNTS_FILE + "'. " + e.getMessage());
			System.exit(-1);
		}
		return null;
	}



	public class CloudAccount {


		public String provider;
		public String identity;
		public String credential;
		public String containerName;

		public CloudAccount(String provider, String identity, String credential, String containerName) {
			super();
			this.provider = provider;
			this.identity = identity;
			this.credential = credential;
			this.containerName = containerName;
		}

		public String getProvider() {
			return provider;
		}

		public void setProvider(String provider) {
			this.provider = provider;
		}

		public String getIdentity() {
			return identity;
		}

		public void setIdentity(String identity) {
			this.identity = identity;
		}

		public String getCredential() {
			return credential;
		}

		public void setCredential(String credential) {
			this.credential = credential;
		}

		public String getContainerName() {
			return containerName;
		}

		public void setContainerName(String containerName) {
			this.containerName = containerName;
		}

		@Override
		public String toString() {
			String result = "cloud_access_credential:{\n";
			result += "\t provider: " + provider + "\n";
			result += "\t identity: " + identity + "\n";
			result += "\t credential: " + credential + "\n";
			result += "\t containerName: " + containerName + "\n";
			result += "}";
			return result;
		}

	}

}
