# SafeCloudFS Configuration file

The SafeCloud FS configuration file is a properties file (in the format propertyName=propertyValue). By default it located in `config/safecloudfs.properties`. Below there's an explanation of the configuration properties (with examples of possible values inside the brackets).

* filesystem.protocol=[`DepSky-A` | `DepSky-CA`]
    - `DepSky-A`: This protocol replicates all the data in clear text in each cloud.
    - `DepSky-CA`: This protocol uses secret sharing and erasure code techniques to replicate the data in a cloud-of-clouds. First is generated an encryption key, and after that the original data block is encrypted. Then the encrypted data block is erasure coded and are computed key shares of the encryption key. In this case we get four erasure coded blocks and four key shares because we use four clouds. Lastly, is stored in each cloud a different coded block together with a different key share.

* upload.method=[`sync` | `async`]
    - `sync`:  it will lock until data was uploaded to the clouds
    - `async`: it will upload data concurrently with the execution of the file system, allowing several upload tasks to be performed at once

* clouds.f=[`1 .. (N/3)-1`]
    - the number of storage clouds that may fail arbitrarily. This number must be lower than a third of the total number of clouds present in the `config/accounts.json` file.

* depspace.config=[`/path/to/configDir`]
    - The location of the config folder of DepSpace. If this value is set then the zookeeper.host cannot be set, since there can only be a single coordination service for SafeCloudFS.

* zookeeper.host=[`192.168.0.5,192.168.0.6`]
    - The IP addresses of ZooKeeper. If this value is set then the depspace.hosts.file cannot be set, since there can only be a single coordination service for SafeCloudFS.

* access.keys.file=[`config/accounts.json`]
    - This JSON file contains the access keys to the storage clouds cloud.

* cache.dir=[`/path/to/cacheDir`]
    - This optional folder can be used to cache the file systems files locally, reducing the number of requests to the storage clouds. If the the file in the cloud is newer than the cached file, then SafeCloudFS downloads the most recent version from the storage clouds.

* recovery.gui=[`true` | `false`]
    - If set to true then a graphical user interface will be presented with the log of operations allowing the user to revert files to previous versions.