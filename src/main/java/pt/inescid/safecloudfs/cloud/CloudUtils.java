package pt.inescid.safecloudfs.cloud;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.contains;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
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
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;
import org.jclouds.s3.S3ApiMetadata;
import org.jclouds.s3.S3Client;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;

import pt.inescid.safecloudfs.utils.CloudAccounts;
import pt.inescid.safecloudfs.utils.CloudAccounts.CloudAccount;
import pt.inescid.safecloudfs.utils.SafeCloudFSProperties;
import pt.inescid.safecloudfs.utils.SafeCloudFSUtils;

public class CloudUtils {
	public static final Map<String, ApiMetadata> allApis = Maps.uniqueIndex(Apis.viewableAs(BlobStoreContext.class),
			Apis.idFunction());

	public static final Map<String, ProviderMetadata> appProviders = Maps
			.uniqueIndex(Providers.viewableAs(BlobStoreContext.class), Providers.idFunction());

	public static final Set<String> allKeys = ImmutableSet
			.copyOf(Iterables.concat(appProviders.keySet(), allApis.keySet()));

	public static int PARAMETERS = 4;
	public static String INVALID_SYNTAX = "Invalid number of parameters. Syntax is: \"provider\" \"identity\" \"credential\" \"containerName\".";

	public static CloudBroker pingClouds() {
		String containerName = "safecloudfsping";
		CloudAccount[] cloudAccounts = CloudAccounts.getAllAccounts();

		if (cloudAccounts.length != 1) {
			if (cloudAccounts.length < (3 * SafeCloudFSProperties.cloudsF + 1)) {
				System.err.println("To tolerate f=" + SafeCloudFSProperties.cloudsF
						+ " faulty clouds it is necessary to setup at least " + (3 * SafeCloudFSProperties.cloudsF + 1)
						+ " clouds in the accounts.json file");
				System.exit(-1);
			}
		}

		SafeCloudFSProperties.cloudsN = cloudAccounts.length;

		if (SafeCloudFSUtils.cloudContexts == null) {
			SafeCloudFSUtils.cloudContexts = new BlobStoreContext[CloudAccounts.getAccountsSize()];
		}

		for (int i = 0; i < cloudAccounts.length; i++) {

			CloudAccount account = cloudAccounts[i];

			checkArgument(contains(allKeys, account.provider), "provider %s not in supported list: %s",
					account.provider, allKeys);









			SafeCloudFSUtils.cloudContexts[i] = null;


			if(account.endpoint != null) {
				Properties overrides = new Properties();
				overrides.setProperty("jclouds.endpoint",  account.endpoint);
				overrides.setProperty("jclouds.trust-all-certs", "true");
				overrides.setProperty("jclouds.relax-hostname", "true");

				SafeCloudFSUtils.cloudContexts[i] = ContextBuilder.newBuilder(account.provider)
						.credentials(account.identity, account.credential).endpoint(account.endpoint).overrides(overrides).buildView(BlobStoreContext.class);

			}else {
				SafeCloudFSUtils.cloudContexts[i] = ContextBuilder.newBuilder(account.provider)
						.credentials(account.identity, account.credential).buildView(BlobStoreContext.class);
			}




			BlobStore blobStore = null;
			try {
				ApiMetadata apiMetadata = SafeCloudFSUtils.cloudContexts[i].unwrap().getProviderMetadata()
						.getApiMetadata();
				blobStore = SafeCloudFSUtils.cloudContexts[i].getBlobStore();
				// Create Container
				Location location = null;
				if (apiMetadata instanceof SwiftApiMetadata) {
					location = Iterables.getFirst(blobStore.listAssignableLocations(), null);
				}
				blobStore.createContainerInLocation(location, containerName);

				String blobName = "ping";

				byte[] byteArray = "ping".getBytes();

				ByteSource payload = ByteSource.wrap(byteArray);

				// Add Blob
				Blob blob = null;
				try {
					blob = blobStore.blobBuilder(blobName).payload(payload).contentLength(payload.size()).build();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
				blobStore.putBlob(containerName, blob);

				// Use Provider API
				Object object = null;
				if (apiMetadata instanceof S3ApiMetadata) {
					S3Client api = SafeCloudFSUtils.cloudContexts[i].unwrapApi(S3Client.class);
					object = api.headObject(containerName, blobName);
				} else if (apiMetadata instanceof SwiftApiMetadata) {
					SwiftApi api = SafeCloudFSUtils.cloudContexts[i].unwrapApi(SwiftApi.class);
					object = api.getObjectApi(location.getId(), containerName).getWithoutBody(blobName);
				} else if (apiMetadata instanceof AzureBlobApiMetadata) {
					AzureBlobClient api = SafeCloudFSUtils.cloudContexts[i].unwrapApi(AzureBlobClient.class);
					object = api.getBlobProperties(containerName, blobName);
				} else if (apiMetadata instanceof AtmosApiMetadata) {
					AtmosClient api = SafeCloudFSUtils.cloudContexts[i].unwrapApi(AtmosClient.class);
					object = api.headFile(containerName + "/" + blobName);
				} else if (apiMetadata instanceof GoogleCloudStorageApiMetadata) {
					GoogleCloudStorageApi api = SafeCloudFSUtils.cloudContexts[i]
							.unwrapApi(GoogleCloudStorageApi.class);
					object = api.getObjectApi().getObject(containerName, blobName);
				}

			} catch (Exception e) {
				e.printStackTrace();
				SafeCloudFSUtils.LOGGER.severe(e.getMessage());
				System.exit(-1);

			} finally {
				// delete cointainer
				blobStore.deleteContainer(containerName);
				// Close connecton
				// RockFSUtils.cloudContexts[i].close();
			}

		}

		CloudBroker cloudUploader = null;

		if (SafeCloudFSProperties.cloudsN == 1) {
			cloudUploader = new SingleCloudBroker(SafeCloudFSProperties.uploadMethod,
					SafeCloudFSUtils.cloudContexts[0]);
		} else {
			cloudUploader = new MultipleCloudBroker(SafeCloudFSProperties.uploadMethod,
					SafeCloudFSUtils.cloudContexts);
		}

		return cloudUploader;

	}

}
