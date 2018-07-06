# SafeCloudFS

SafeCloudFS -- is a File System supported by a single cloud or cloud-of-clouds resilient to client side attacks.

SafeCloudFS provides two sets of security mechanisms to be integrated with the client-side of a file system:
 * a *recovery service* capable of undoing unintended file operations without losing valid file operations that occurred after the attack; and
 * *device data security mechanisms* to safely store encryption keys reducing the probability of having the credentials compromised by attackers and to protect cached data.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

SafeCloudFS uses FUSE library. You need  to install the following tools before using SafeCloudFS.


| Supported platforms                                           |     |      |
|---------------------------------------------------------------|-----|------|
| Linux (sudo apt-get install libfuse-dev)                      | x64 | x86  |
| MacOS (via [osxfuse](https://osxfuse.github.io/))             | x64 | x86  |
| Windows (via [winfsp](https://github.com/billziss-gh/winfsp/))| x64 | n/a  |

Please make sure you have at least Java 7 installed and Maven.

### Installing

Before running SafeCloudFS execute the following command to install the required libs to you local Maven repository.

```
sh install.sh
```


## Running SafeCloudFS

Before executing SafeCloudFS fill the cofiguration files in `config` folder.

Execution arguments are set in the pom.xml file.

### Arguments

* --mount [path] - Directory to be mount
* --config [path] - Config file path
* --accessKeys [path] -  JSON file with cloud access keys file
* --depspace [path] - Depspace hosts file
* --zookeeper <IPAddress> - Zookeeper server address
* -- debug <ALL, SIMPLE, WARNING, SEVERE, INFO, FINE, FINER, FINEST> - Execute with debug log messages
* --recovery - Opens a GUI that allows intrusion recovery
* --cache - Path to a folder to store cached files

### Running localy
```
mvn exec:java
```


### Running via Docker
```
docker build -t safecloudfs .
docker run -it --privileged --cap-add SYS_ADMIN --device /dev/fuse -i safecloudfs
```
Then inside the container execute
```
mvn exec:java
```


## Built With

* [Google](http://www.dropwizard.io/1.0.2/docs/) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management
* [JBDiff](https://github.com/jdesbonnet/jbdiff) - Used to create log entries of users' operations

## Authors

* **David R. Matos** - *Development* - [GitHub](https://github.com/davidmatos)
* **Prof. Miguel Correia**
* **Prof. Miguel L. Pardal**
* **Prof. Georg Carle**

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Prof. Miguel Correia, Prof. Miguel L. Pardal and Prof. Georg Carle 
* Alysson Bessani for the development of the PVSS lib used;
* The contributors of the [SCFS](https://github.com/cloud-of-clouds/SCFS), [DepSky](https://github.com/cloud-of-clouds/depsky), [DepSpace](https://github.com/bft-smart/depspace) and [SMaRT-BFT](https://github.com/bft-smart/library) projects.

