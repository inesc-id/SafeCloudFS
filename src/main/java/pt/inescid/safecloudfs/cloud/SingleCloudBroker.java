package pt.inescid.safecloudfs.cloud;

import static java.lang.Math.toIntExact;

import java.io.InputStream;

import org.jclouds.apis.ApiMetadata;
import org.jclouds.atmos.AtmosApiMetadata;
import org.jclouds.atmos.AtmosClient;
import org.jclouds.azureblob.AzureBlobApiMetadata;
import org.jclouds.azureblob.AzureBlobClient;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.domain.Location;
import org.jclouds.googlecloudstorage.GoogleCloudStorageApi;
import org.jclouds.googlecloudstorage.GoogleCloudStorageApiMetadata;
import org.jclouds.openstack.swift.v1.SwiftApi;
import org.jclouds.openstack.swift.v1.SwiftApiMetadata;
import org.jclouds.s3.S3ApiMetadata;
import org.jclouds.s3.S3Client;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;

import pt.inescid.safecloudfs.utils.CloudAccounts;
import pt.inescid.safecloudfs.utils.CloudAccounts.CloudAccount;

public class SingleCloudBroker implements CloudBroker {

	public boolean sync;
	private BlobStoreContext cloudContext;

	public SingleCloudBroker(boolean sync, BlobStoreContext cloudContext) {
		this.sync = sync;
		this.cloudContext = cloudContext;
	}

	@Override
	public void upload(String path, byte[] byteArray) {
		if (sync) {
			uploadSync(path, byteArray);
		} else {
			uploadAsync(path, byteArray);
		}

	}

	public void uploadAsync(String path, byte[] byteArray) {

		try {
			UploadWorker uploadWorker = new UploadWorker(path, byteArray);

			new Thread(uploadWorker).start();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void uploadSync(String path, byte[] byteArray) {
		try {
			UploadWorker uploadWorker = new UploadWorker(path, byteArray);

			new Thread(uploadWorker).start();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void uploadByteArray(String path, byte[] byteArray) {

		CloudAccount[] cloudAccounts = CloudAccounts.getAllAccounts();

		CloudAccount account = cloudAccounts[0];

		try {
			BlobStore blobStore = null;

			ApiMetadata apiMetadata = cloudContext.unwrap().getProviderMetadata().getApiMetadata();
			blobStore = cloudContext.getBlobStore();
			// Create Container
			Location location = null;
			if (apiMetadata instanceof SwiftApiMetadata) {
				location = Iterables.getFirst(blobStore.listAssignableLocations(), null);
			}
			blobStore.createContainerInLocation(location, account.containerName);
			String blobName = path;

			ByteSource payload = ByteSource.wrap(byteArray);



			// Add Blob
			Blob blob = blobStore.blobBuilder(blobName).payload(payload).contentLength(payload.size()).build();
			blobStore.putBlob(account.containerName, blob);

			// Use Provider API
			Object object = null;
			if (apiMetadata instanceof S3ApiMetadata) {
				S3Client api = cloudContext.unwrapApi(S3Client.class);
				object = api.headObject(account.containerName, blobName);

			} else if (apiMetadata instanceof SwiftApiMetadata) {
				SwiftApi api = cloudContext.unwrapApi(SwiftApi.class);
				object = api.getObjectApi(location.getId(), account.containerName).getWithoutBody(blobName);
			} else if (apiMetadata instanceof AzureBlobApiMetadata) {
				AzureBlobClient api = cloudContext.unwrapApi(AzureBlobClient.class);
				object = api.getBlobProperties(account.containerName, blobName);
			} else if (apiMetadata instanceof AtmosApiMetadata) {
				AtmosClient api = cloudContext.unwrapApi(AtmosClient.class);
				object = api.headFile(account.containerName + "/" + blobName);
			} else if (apiMetadata instanceof GoogleCloudStorageApiMetadata) {
				GoogleCloudStorageApi api = cloudContext.unwrapApi(GoogleCloudStorageApi.class);
				object = api.getObjectApi().getObject(account.containerName, blobName);
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

		private String path;
		private byte[] byteArray;

		public UploadWorker(String path, byte[] byteArray) {
			this.path = path;
			this.byteArray = byteArray;
		}

		public void run() {
			uploadByteArray(path, byteArray);

		}

	}

	@Override
	public void remove(String path) {
		if (sync) {
			removeSync(path);
		} else {
			removeAsync(path);
		}

	}

	private void removeFromCloud(String path) {
		CloudAccount[] cloudAccounts = CloudAccounts.getAllAccounts();

		CloudAccount account = cloudAccounts[0];

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

	private void removeSync(String path) {
		removeFromCloud(path);
	}

	private void removeAsync(String path) {
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
			removeFromCloud(path);

		}

	}

	@Override
	public byte[] download(String path) {
		CloudAccount[] cloudAccounts = CloudAccounts.getAllAccounts();

		CloudAccount account = cloudAccounts[0];

		byte[] bytes = null;
		try {
			BlobStore blobStore = null;


			ApiMetadata apiMetadata = cloudContext.unwrap().getProviderMetadata()
					.getApiMetadata();
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
				size = toIntExact(api.getObjectApi(location.getId(), account.containerName).get(path).getPayload().openStream().available());
			} else if (apiMetadata instanceof AzureBlobApiMetadata) {
				AzureBlobClient api = cloudContext.unwrapApi(AzureBlobClient.class);
				size = toIntExact(api.getBlobProperties(account.containerName, path).getContentMetadata().getContentLength());
			} else if (apiMetadata instanceof AtmosApiMetadata) {
				AtmosClient api = cloudContext.unwrapApi(AtmosClient.class);
				size = toIntExact(api.headFile(account.containerName + "/" + path).getContentMetadata().getContentLength());
			} else if (apiMetadata instanceof GoogleCloudStorageApiMetadata) {
				GoogleCloudStorageApi api = cloudContext
						.unwrapApi(GoogleCloudStorageApi.class);
				size = toIntExact(api.getObjectApi().getObject(account.containerName, path).size());
			}

			InputStream is = blob.getPayload().openStream();


			bytes = new byte[ size ];
			is.read(bytes);
			is.close();


		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return bytes;
	}

}
