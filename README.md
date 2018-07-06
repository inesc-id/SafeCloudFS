# RockFS

ROCKFS -- RecOverable Cloud-bacKed File-System, is a File System supported by a single cloud or cloud-of-clouds resilient to client side attacks.

RockFS provides two sets of security mechanisms to be integrated with the client-side of a file system:
 * a *recovery service* capable of undoing unintended file operations without losing valid file operations that occurred after the attack; and
 * *device data security mechanisms* to safely store encryption keys reducing the probability of having the credentials compromised by attackers and to protect cached data.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

RockFS uses FUSE library. You need  to install the following tools before using RockFS.


| Supported platforms                                           |     |      |
|---------------------------------------------------------------|-----|------|
| Linux (sudo apt-get install libfuse-dev)                      | x64 | x86  |
| MacOS (via [osxfuse](https://osxfuse.github.io/))             | x64 | x86  |
| Windows (via [winfsp](https://github.com/billziss-gh/winfsp/))| x64 | n/a  |

Please make sure you have at least Java 7 installed and Maven.

### Installing

Before running RockFS execute the following command to install the required libs to you local Maven repository.

```
sh install.sh
```

## Running the tests

```
mvn test
```

### Break down into end to end tests

Explain what these tests test and why

```
Give an example
```

### And coding style tests

Explain what these tests test and why

```
Give an example
```

## Deployment

Add additional notes about how to deploy this on a live system

## Built With

* [Google](http://www.dropwizard.io/1.0.2/docs/) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management
* [JBDiff](https://github.com/jdesbonnet/jbdiff) - Used to create log entries of users' operations

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags).

## Authors

* **David R. Matos** - *Development* - [GitHub](https://github.com/davidmatos)

See also the list of [contributors](https://github.com/davidmatos/rockfs/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Prof. Miguel Correia, Prof. Miguel L. Pardal and Prof. Georg Carle - CoAuthors of the article RockFS: Cloud-backed File System Resilience to Client-Side Attacks
* Alysson Bessani for the development of the PVSS lib used;
* The contributors of the [SCFS](https://github.com/cloud-of-clouds/SCFS), [DepSky](https://github.com/cloud-of-clouds/depsky), [DepSpace](https://github.com/bft-smart/depspace) and [SMaRT-BFT](https://github.com/bft-smart/library) projects.

