package pt.inescid.safecloudfs.cloud;

import static java.lang.Math.toIntExact;

import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

import org.jclouds.apis.ApiMetadata;
import org.jclouds.atmos.AtmosApiMetadata;
import org.jclouds.atmos.AtmosClient;
import org.jclouds.azureblob.AzureBlobApiMetadata;
import org.jclouds.azureblob.AzureBlobClient;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.domain.Location;
import org.jclouds.googlecloudstorage.GoogleCloudStorageApi;
import org.jclouds.googlecloudstorage.GoogleCloudStorageApiMetadata;
import org.jclouds.openstack.swift.v1.SwiftApi;
import org.jclouds.openstack.swift.v1.SwiftApiMetadata;
import org.jclouds.s3.S3ApiMetadata;
import org.jclouds.s3.S3Client;

import com.codahale.shamir.Scheme;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;

//import net.schmizz.sshj.common.Base64;
import pt.inescid.safecloudfs.security.AES;
import pt.inescid.safecloudfs.security.ErasureCodes;
import pt.inescid.safecloudfs.utils.CloudAccounts;
import pt.inescid.safecloudfs.utils.CloudAccounts.CloudAccount;
import pt.inescid.safecloudfs.utils.SafeCloudFSProperties;
import pt.inescid.safecloudfs.utils.SafeCloudFSUtils;

public class MultipleCloudBroker implements CloudBroker {

	public boolean sync;
	private BlobStoreContext[] cloudContexts;

	final static Scheme scheme = Scheme.of(SafeCloudFSProperties.CLOUDS_N,
			SafeCloudFSProperties.CLOUDS_N - SafeCloudFSProperties.CLOUDS_F);

	public MultipleCloudBroker(boolean sync, BlobStoreContext[] cloudContexts) {
		this.sync = sync;
		this.cloudContexts = cloudContexts;
	}

	@Override
	public void upload(String path, byte[] byteArray) {
		if (sync) {
			uploadSync(path, byteArray);
		} else {
			uploadAsync(path, byteArray);
		}

	}

	private void uploadAsync(String path, byte[] byteArray) {

		if (SafeCloudFSProperties.FILESYSTEM_PROTOCOL == SafeCloudFSProperties.DEPSKY_A) {
			protocolAUploadAsync(path, byteArray);
		} else {
			protocolCAUploadAsync(path, byteArray);
		}

	}

	private void protocolAUploadAsync(String path, byte[] byteArray) {
		try {
			UploadWorker[] workers = new UploadWorker[cloudContexts.length];
			for (int i = 0; i < cloudContexts.length; i++) {
				workers[i] = new UploadWorker(i, path, byteArray);
				new Thread(workers[i]).start();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void protocolCAUploadAsync(String path, byte[] byteArray) {

		// 1 - First we encrypt the byte array using AES

		SecretKey secretKey = AES.getSecretKey();
		byte[] encryptedByteArray = AES.encrypt(byteArray, secretKey);

		// 2 - Now we break the secret using the PVSS protocol
		// PVSSEngine.
		// TODO: Usar valores de acordo com o setup

//		 PVSSEngine pvssEngine = PVSSEngine.getInstance(4, 2, 192);
		//// pvssEngine.

		final Map<Integer, byte[]> parts = scheme.split(secretKey.getEncoded());

		// 3 - Now we break the data blocks using erasure codes
		byte[][] dataBlocks = ErasureCodes.encode(encryptedByteArray, SafeCloudFSProperties.CLOUDS_N- SafeCloudFSProperties.CLOUDS_F, SafeCloudFSProperties.CLOUDS_F);

		try {
			// Upload payload
			UploadWorker[] workers = new UploadWorker[cloudContexts.length];
			for (int i = 0; i < dataBlocks.length; i++) {
				workers[i] = new UploadWorker(i, path, dataBlocks[i]);

				byte[] encodedBytes = Base64.getEncoder().encode(dataBlocks[i]);
				System.out.println("encodedBytes["+i+"] = " + new String(encodedBytes));

				new Thread(workers[i]).start();
			}

			// Upload keys
			UploadWorker[] workersKeys = new UploadWorker[cloudContexts.length];
			for (int i = 0; i < dataBlocks.length; i++) {
				workersKeys[i] = new UploadWorker(i, SafeCloudFSUtils.getKeyName(path), parts.get(i));
				new Thread(workersKeys[i]).start();
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void uploadSync(String path, byte[] byteArray) {

		if (SafeCloudFSProperties.FILESYSTEM_PROTOCOL == SafeCloudFSProperties.DEPSKY_A) {
			protocolAUploadSync(path, byteArray);
		} else {
			protocolCAUploadSync(path, byteArray);
		}

	}

	private void protocolAUploadSync(String path, byte[] byteArray) {
		try {

			for (int i = 0; i < cloudContexts.length; i++) {
				uploadByteArray(i, path, byteArray);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void protocolCAUploadSync(String path, byte[] byteArray) {

		// 1 - First we encrypt the byte array using AES

		SecretKey secretKey = AES.getSecretKey();

		byte[] encryptedByteArray = AES.encrypt(byteArray, secretKey);

		// 2 - Now we break the secret using the PVSS protocol
		// final Scheme scheme = Scheme.of(SafeCloudFSProperties.CLOUDS_N,
		// SafeCloudFSProperties.CLOUDS_N - SafeCloudFSProperties.CLOUDS_F);

		final Map<Integer, byte[]> parts = scheme.split(secretKey.getEncoded());

		Set<Integer> keys = parts.keySet();
		for (Integer key : keys) {
			System.out.println("k=" + key + " v=" + new String(Base64.getEncoder().encode(parts.get(key))));
		}

		// 3 - Now we break the data blocks using erasure codes
		byte[][] dataBlocks = null;
		try {
			dataBlocks = ErasureCodes.encode(encryptedByteArray, SafeCloudFSProperties.CLOUDS_N- SafeCloudFSProperties.CLOUDS_F, SafeCloudFSProperties.CLOUDS_F);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			// Upload payload

			for (int i = 0; i < dataBlocks.length; i++) {
				uploadByteArray(i, path, dataBlocks[i]);

				byte[] encodedBytes = Base64.getEncoder().encode(dataBlocks[i]);
				System.out.println("encodedBytes["+i+"] = " + new String(encodedBytes));


				Integer key = new Integer(i + 1);
				uploadByteArray(i, SafeCloudFSUtils.getKeyName(path), parts.get(key));
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void uploadByteArray(int cloudId, String path, byte[] byteArray) {
		SafeCloudFSUtils.LOGGER.info("Uploading to the clouds");

		CloudAccount[] cloudAccounts = CloudAccounts.getAllAccounts();

		CloudAccount account = cloudAccounts[cloudId];

		try {
			BlobStore blobStore = null;

			ApiMetadata apiMetadata = cloudContexts[cloudId].unwrap().getProviderMetadata().getApiMetadata();
			blobStore = cloudContexts[cloudId].getBlobStore();
			// Create Container
			Location location = null;
			if (apiMetadata instanceof SwiftApiMetadata) {
				location = Iterables.getFirst(blobStore.listAssignableLocations(), null);
			}
			blobStore.createContainerInLocation(location, account.containerName);
			String blobName = path;

			ByteSource payload = ByteSource.wrap(byteArray);

			// List Container Metadata
			for (StorageMetadata resourceMd : blobStore.list()) {
				if (account.containerName.equals(resourceMd.getName())) {
					System.out.println(resourceMd);
				}
			}

			// Add Blob
			Blob blob = blobStore.blobBuilder(blobName).payload(payload).contentLength(payload.size()).build();
			blobStore.putBlob(account.containerName, blob);

			// Use Provider API
			Object object = null;
			if (apiMetadata instanceof S3ApiMetadata) {
				S3Client api = cloudContexts[cloudId].unwrapApi(S3Client.class);
				object = api.headObject(account.containerName, blobName);

			} else if (apiMetadata instanceof SwiftApiMetadata) {
				SwiftApi api = cloudContexts[cloudId].unwrapApi(SwiftApi.class);
				object = api.getObjectApi(location.getId(), account.containerName).getWithoutBody(blobName);
			} else if (apiMetadata instanceof AzureBlobApiMetadata) {
				AzureBlobClient api = cloudContexts[cloudId].unwrapApi(AzureBlobClient.class);
				object = api.getBlobProperties(account.containerName, blobName);
			} else if (apiMetadata instanceof AtmosApiMetadata) {
				AtmosClient api = cloudContexts[cloudId].unwrapApi(AtmosClient.class);
				object = api.headFile(account.containerName + "/" + blobName);
			} else if (apiMetadata instanceof GoogleCloudStorageApiMetadata) {
				GoogleCloudStorageApi api = cloudContexts[cloudId].unwrapApi(GoogleCloudStorageApi.class);
				object = api.getObjectApi().getObject(account.containerName, blobName);
			}
			if (object != null) {
				System.out.println(object);
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		} finally {
			// delete cointainer
			// blobStore.deleteContainer(containerName);
			// Close connecton
			// context.close();
		}

	}

	class UploadWorker implements Runnable {

		private int cloudId;
		private String path;
		private byte[] byteArray;

		public UploadWorker(int cloudId, String path, byte[] byteArray) {
			this.cloudId = cloudId;
			this.path = path;
			this.byteArray = byteArray;
		}

		public void run() {
			uploadByteArray(cloudId, path, byteArray);

		}

	}

	@Override
	public void remove(String path) {
		System.out.println("Will now remove file: " + path);
		if (sync) {
			removeSync(path);
			removeSync(SafeCloudFSUtils.getKeyName(path));
		} else {
			removeAsync(path);
			removeAsync(SafeCloudFSUtils.getKeyName(path));
		}

	}

	private void removeFromClouds(String path) {
		CloudAccount[] cloudAccounts = CloudAccounts.getAllAccounts();

		for (int i = 0; i < cloudAccounts.length; i++) {

			CloudAccount account = cloudAccounts[i];
			BlobStoreContext cloudContext = cloudContexts[i];

			try {
				BlobStore blobStore = null;

				ApiMetadata apiMetadata = cloudContext.unwrap().getProviderMetadata().getApiMetadata();
				blobStore = cloudContext.getBlobStore();

				if (apiMetadata instanceof S3ApiMetadata) {
					S3Client api = cloudContext.unwrapApi(S3Client.class);
					api.deleteObject(account.containerName, path);

				} else if (apiMetadata instanceof SwiftApiMetadata) {
					SwiftApi api = cloudContext.unwrapApi(SwiftApi.class);
					Location location = null;
					if (apiMetadata instanceof SwiftApiMetadata) {
						location = Iterables.getFirst(blobStore.listAssignableLocations(), null);
					}
					api.getObjectApi(location.getId(), account.containerName).delete(path);
				} else if (apiMetadata instanceof AzureBlobApiMetadata) {
					AzureBlobClient api = cloudContext.unwrapApi(AzureBlobClient.class);
					api.deleteBlob(account.containerName, path);
				} else if (apiMetadata instanceof AtmosApiMetadata) {
					AtmosClient api = cloudContext.unwrapApi(AtmosClient.class);
					api.deletePath(account.containerName + "/" + path);
				} else if (apiMetadata instanceof GoogleCloudStorageApiMetadata) {
					GoogleCloudStorageApi api = cloudContext.unwrapApi(GoogleCloudStorageApi.class);
					api.getObjectApi().deleteObject(account.containerName, path);
				}

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	private void removeSync(String path) {
		System.out.println("Sync");
		removeFromClouds(path);
	}

	private void removeAsync(String path) {
		System.out.println("Async");
		try {
			RemoveWorker removeWorker = new RemoveWorker(path);

			new Thread(removeWorker).start();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	class RemoveWorker implements Runnable {

		private String path;

		public RemoveWorker(String path) {
			this.path = path;
		}

		public void run() {
			removeFromClouds(path);

		}

	}

	@Override
	public byte[] download(String path) {
		CloudAccount[] cloudAccounts = CloudAccounts.getAllAccounts();

		byte[][] dataParts = new byte[4][0];
		HashMap<Integer, byte[]> keyParts = new HashMap<>();

		for (int i = 0; i < cloudAccounts.length; i++) {
			dataParts[i] = dowloadBlob(path, i);
			keyParts.put(new Integer(i+1), dowloadBlob(SafeCloudFSUtils.getKeyName(path), i));


			byte[] encodedBytes = Base64.getEncoder().encode(dataParts[i]);
			System.out.println("encodedBytes["+i+"] = " + new String(encodedBytes));

		}

		byte[] key = scheme.join(keyParts);
		System.out.println("This is the key: " + new String(Base64.getEncoder().encode(key) ));
		byte[] dataCiphered = ErasureCodes.decode(dataParts, SafeCloudFSProperties.CLOUDS_N- SafeCloudFSProperties.CLOUDS_F, SafeCloudFSProperties.CLOUDS_F);

		return AES.decrypt(dataCiphered, AES.getSecretKeyFromBytes(key));
	}

	private byte[] dowloadBlob(String path, int i) {

		CloudAccount[] cloudAccounts = CloudAccounts.getAllAccounts();
		CloudAccount account = cloudAccounts[i];

		BlobStoreContext cloudContext = cloudContexts[i];

		byte[] bytes = null;
		try {
			BlobStore blobStore = null;

			ApiMetadata apiMetadata = cloudContext.unwrap().getProviderMetadata().getApiMetadata();
			blobStore = cloudContext.getBlobStore();
			// Create Container
			Location location = null;
			if (apiMetadata instanceof SwiftApiMetadata) {
				location = Iterables.getFirst(blobStore.listAssignableLocations(), null);
			}

			Blob blob = blobStore.getBlob(account.containerName, path);

			int size = 0;

			if (apiMetadata instanceof S3ApiMetadata) {
				S3Client api = cloudContext.unwrapApi(S3Client.class);
				size = toIntExact(api.headObject(account.containerName, path).getContentMetadata().getContentLength());
			} else if (apiMetadata instanceof SwiftApiMetadata) {
				SwiftApi api = cloudContext.unwrapApi(SwiftApi.class);
				size = toIntExact(api.getObjectApi(location.getId(), account.containerName).get(path).getPayload()
						.openStream().available());
			} else if (apiMetadata instanceof AzureBlobApiMetadata) {
				AzureBlobClient api = cloudContext.unwrapApi(AzureBlobClient.class);
				size = toIntExact(
						api.getBlobProperties(account.containerName, path).getContentMetadata().getContentLength());
			} else if (apiMetadata instanceof AtmosApiMetadata) {
				AtmosClient api = cloudContext.unwrapApi(AtmosClient.class);
				size = toIntExact(
						api.headFile(account.containerName + "/" + path).getContentMetadata().getContentLength());
			} else if (apiMetadata instanceof GoogleCloudStorageApiMetadata) {
				GoogleCloudStorageApi api = cloudContext.unwrapApi(GoogleCloudStorageApi.class);
				size = toIntExact(api.getObjectApi().getObject(account.containerName, path).size());
			}

			InputStream is = blob.getPayload().openStream();

			// System.out.println("SIZE=" +
			// object.getMetadata().getContentMetadata().getContentLength());

			bytes = new byte[size];
			is.read(bytes);
			is.close();

			// System.out.println("exists=" + object.getPayload().toString());

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return bytes;

		// return null;
	}
}
