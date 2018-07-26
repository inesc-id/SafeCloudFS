package pt.inescid.safecloudfs.directoryService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

import depspace.client.DepSpaceAccessor;
import depspace.client.DepSpaceAdmin;
import depspace.general.DepSpaceConfiguration;
import depspace.general.DepSpaceException;
import depspace.general.DepSpaceProperties;
import depspace.general.DepTuple;
import pt.inescid.safecloudfs.security.SHA;
import pt.inescid.safecloudfs.utils.SafeCloudFSConstants;

public class DepSpaceDirectoryService implements DirectoryService {

	private static final String FILE_SYSTEM_ENTRY_ID = "FILE_SYSTEM_ENTRY";


	private DepSpaceAccessor accessor;

	public DepSpaceDirectoryService(String hostsFile) {

		String name = "SafeCloudFS";

		DepSpaceConfiguration.init(hostsFile);

		// the DepSpace name
		Properties prop = DepSpaceProperties.createDefaultProperties(name);

		// use confidentiality?
		prop.put(DepSpaceProperties.DPS_CONFIDEALITY, "false");

		int clientID = 4;

		// the DepSpace Accessor, who will access the DepSpace.
		accessor = null;
		try {
			accessor = new DepSpaceAdmin(clientID).createSpace(prop);


			FileSystemEntry fse = new FileSystemEntry("/", true, 0, Calendar.getInstance().getTimeInMillis(), 1);

			accessor.out(fse.toTuple());

			DepTuple template = DepTuple.createTuple(new Object[] { "nlink", "*" });


			if (accessor.rdAll(template, 0).isEmpty()) {
				DepTuple tupleNlink = DepTuple.createTuple(new Object[] { "nlink", "1" });
				accessor.out(tupleNlink);
			}


			Object[] temp  = fse.toTuple().getFields();
			for(int i = 0; i < temp.length; i++) {
				temp[i] = "*";
			}


		} catch (DepSpaceException e) {
			System.out.println("Couldn't connect to DepSpace. Will retry again.");

		}
	}

	@Override
	public void mv(String originPath, String destinationPath) {
		try {
			FileSystemEntry originEntry = new FileSystemEntry(originPath);
			FileSystemEntry destinationEntry = new FileSystemEntry(destinationPath, originEntry.isDir, originEntry.mode,
					originEntry.ts, originEntry.nlink);
			originEntry.replace(destinationEntry);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void rm(String path) {
		try {
			FileSystemEntry originEntry = new FileSystemEntry(path);
			originEntry.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void rmdir(String path) {
		try {

			Object[] pathPartsTemp = path.split("/");
			Object[]pathParts = new Object[pathPartsTemp.length-1];
			System.arraycopy(pathPartsTemp, 1, pathParts, 0, pathParts.length);

			pathParts = appendToBeginOfArray(pathParts, "/");



			for (int i = 6+pathParts.length; i < SafeCloudFSConstants.MAX_DEPTH+6; i++) {

				Object[] templateFields = new Object[i];

				templateFields[0] = FILE_SYSTEM_ENTRY_ID;
				templateFields[1] = "*";
				templateFields[2] = "*";
				templateFields[3] = "*";
				templateFields[4] = "*";
				templateFields[5] = "*";

				System.arraycopy(pathParts, 0, templateFields, 6, pathParts.length);
				for (int j = 6+pathParts.length; j < i; j++) {
					templateFields[j] = "*";
				}

				DepTuple template = DepTuple.createTuple(templateFields);



				try {
					accessor.inAll(template);
				} catch (DepSpaceException e) {
					System.err.println(e.getMessage());
					System.exit(-1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void mkfile(String path) {
		try {
			FileSystemEntry fse = new FileSystemEntry(path, false, 0, Calendar.getInstance().getTimeInMillis(),
					getNewNLink());
			DepTuple tuple = fse.toTuple();
			try {
				accessor.out(tuple);
			} catch (DepSpaceException e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void mkdir(String path) {
		try {
			FileSystemEntry fse = new FileSystemEntry(path, true, 0, Calendar.getInstance().getTimeInMillis(),
					getNewNLink());
			DepTuple tuple = fse.toTuple();

			try {
				accessor.out(tuple);
			} catch (DepSpaceException e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public long getNLink(String path) {
		try {
			long result = new FileSystemEntry(path).nlink;
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public Number getMode(String path) {
		try {
			Number result = new FileSystemEntry(path).mode;
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public void setMode(String path, Number mode) {
		try {
			FileSystemEntry entry = new FileSystemEntry(path);
			FileSystemEntry newEntry = new FileSystemEntry(path);
			newEntry.mode = mode;
			entry.replace(newEntry);
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	@Override
	public boolean exists(String path) {
		try {
			boolean result = new FileSystemEntry(path).path != null;
			return result;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean isDir(String path) {
		try {
			boolean result = new FileSystemEntry(path).isDir;
			return result;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public String getParent(String path) {
		try {
			if (path.equals("/")) {
				return null;

			}
			String parentPath = path.substring(0, path.lastIndexOf("/"));
			if (path.lastIndexOf("/") == 0) {
				parentPath = "/";
			}

			return parentPath;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ArrayList<String> readDir(String path) {
		try {
			Object[] pathParts = null;
			if (path.equals("/")) {
				pathParts = new Object[] { "/" };
			} else {
				pathParts = path.startsWith("/") ? path.substring(1).split("/") : path.split("/");
				pathParts = appendToBeginOfArray(pathParts, "/");
			}

			Object[] tupleTemplateFields = new Object[pathParts.length + 6 + 1];


			tupleTemplateFields[0] = FILE_SYSTEM_ENTRY_ID;
			tupleTemplateFields[1] = "*";
			tupleTemplateFields[2] = "*";
			tupleTemplateFields[3] = "*";
			tupleTemplateFields[4] = "*";
			tupleTemplateFields[5] = "*";

			System.arraycopy(pathParts, 0, tupleTemplateFields, 6, pathParts.length);

			tupleTemplateFields[tupleTemplateFields.length-1] = "*";

			DepTuple template = DepTuple.createTuple(tupleTemplateFields);



			ArrayList<DepTuple> children = null;
			try {
				children = new ArrayList<>(accessor.rdAll(template, 0));
			} catch (DepSpaceException e) {
				e.printStackTrace();
			}
			ArrayList<String> result = new ArrayList<>();
			for (DepTuple entry : children) {

				if(entry.getFields().length <= 6+pathParts.length) {
					continue;
				}

				result.add(entry.getFields()[6+pathParts.length].toString());

			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private long getNewNLink() {
		try {

			long nLink = 0;
			try {

				DepTuple template = DepTuple.createTuple(new Object[] { "nlink", "*" });
				DepTuple oldNlink = accessor.rd(template);

				nLink = (Long.parseLong(oldNlink.getFields()[1].toString())) + 1;
				DepTuple newNlink = DepTuple.createTuple(new Object[] { "nlink", nLink });

				accessor.replace(template, newNlink);
			} catch (DepSpaceException e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}
			return nLink;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	public class FileSystemEntry {

		// this string will be present in each file system entry of the tuple space in
		// order to identify that the tuple corresponds to a file system entry


		public String path = null;

		public byte[] hash;
		public long nlink;
		public boolean isDir;
		public Number mode;
		public long ts;

		private final static int POS_HASH = 1;
		private final static int POS_N_LINK = 2;
		private final static int POS_IS_DIR = 3;
		private final static int POS_MODE = 4;
		private final static int POS_TS = 5;
		private final static int POS_FILE_PATH = 6; //it starts here, but it will go on

		public FileSystemEntry(String path, boolean isDir, Number mode, long ts, long nlink) {
			if (path.equals("/")) {
				this.path = path;
			} else {
				this.path = path.substring(1);
			}

			this.isDir = isDir;
			this.mode = mode;
			this.ts = ts;
			this.nlink = nlink;
		}

		public FileSystemEntry(long nLink) {
			try {
				Object[] metadataValues = new Object[] { "*", nLink, "*", "*", "*" };

				for (int i = 2; i < SafeCloudFSConstants.MAX_DEPTH; i++) {

					Object[] pathParts = new Object[i];
					for (int j = 0; j < i; j++) {
						pathParts[j] = "*";
					}
					Object[] tupleFields = new Object[pathParts.length + metadataValues.length];

					System.arraycopy(pathParts, 0, tupleFields, 0, pathParts.length);
					System.arraycopy(metadataValues, 0, tupleFields, pathParts.length, metadataValues.length);

					DepTuple template = DepTuple.createTuple(tupleFields);

					DepTuple tuple = null;
					try {
						tuple = accessor.rdp(template);
					} catch (DepSpaceException e) {
						System.err.println(e.getMessage());
						System.exit(-1);
					}

					if (tuple != null) {
						Object[] fields = tuple.getFields();

						String path = "";
						for (int k = 1; k < fields.length - POS_HASH; k++) {
							path += ("/" + fields[k]);
						}
						this.path = path;
						this.hash = (byte[]) fields[POS_HASH];
						this.nlink = Long.parseLong(fields[POS_N_LINK].toString());
						this.isDir = (boolean) fields[POS_IS_DIR];
						this.mode = (Number) fields[POS_MODE];
						this.ts = (long) fields[POS_TS];
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public FileSystemEntry(String path) {
			try {

				Object[] pathParts = null;
				if (path.equals("/")) {
					pathParts = new Object[] { "/" };
				} else {
					pathParts = path.substring(1).split("/");
					pathParts = appendToBeginOfArray(pathParts, "/");

				}

				Object[] nullValues = new Object[] { FILE_SYSTEM_ENTRY_ID, "*", "*", "*", "*", "*" };

				Object[] tupleFields = new Object[pathParts.length + nullValues.length];

				System.arraycopy(nullValues, 0, tupleFields, 0, nullValues.length);
				System.arraycopy(pathParts, 0, tupleFields, nullValues.length, pathParts.length);



				DepTuple template = DepTuple.createTuple(tupleFields);

				DepTuple tuple = null;
				try {
					tuple = accessor.rdp(template);
				} catch (DepSpaceException e) {
					System.err.println(e.getMessage());
					System.exit(-1);
				}

				if (tuple != null) {
					Object[] fields = tuple.getFields();
					this.path = path;
					this.hash = (byte[]) fields[POS_HASH];
					this.nlink = Long.parseLong(fields[POS_N_LINK].toString());
					this.isDir = (boolean) fields[POS_IS_DIR];
					this.mode = (Number) fields[POS_MODE];
					this.ts = (long) fields[POS_TS];
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public DepTuple toTuple() {
			try {
				Object[] pathParts = null;

				if (path.equals("/")) {
					pathParts = new Object[] { "/" };
				} else {
					path = path.startsWith("/") ? path.substring(1) : path;
					pathParts = path.split("/");
					pathParts = appendToBeginOfArray(pathParts, "/");
				}

				Object[] metadata = new Object[] { FILE_SYSTEM_ENTRY_ID, hash, nlink, isDir, mode, ts };

				Object[] tupleFields = new Object[pathParts.length + metadata.length];

				System.arraycopy(metadata, 0, tupleFields, 0, metadata.length);
				System.arraycopy(pathParts, 0, tupleFields, metadata.length, pathParts.length);


				return DepTuple.createTuple(tupleFields);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

		}

		public void delete() {
			try {

				DepTuple depTuple = this.toTuple();
				Object[] fields = depTuple.getFields();
				for(int i = 0; i < fields.length; i++) {
					if(fields[i] == null) {
						fields[i] = "*";
					}
				}

				depTuple.setFields(fields);
				accessor.in(depTuple);
			} catch (DepSpaceException e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}
		}

		public void replace(FileSystemEntry newEntry) {
			try {
				accessor.replace(this.toTuple(), newEntry.toTuple());
			} catch (DepSpaceException e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}
		}

	}

	/**
	 * This method will add elements to an array and return the resulting array
	 *
	 * @param arr
	 * @param elements
	 * @return
	 */
	private static Object[] appendToBeginOfArray(Object[] arr, Object... elements) {
		Object[] tempArr = new Object[arr.length + elements.length];
		System.arraycopy(arr, 0, tempArr, elements.length, arr.length);

		for (int i = 0; i < elements.length; i++)
			tempArr[i] = elements[i];
		return tempArr;

	}

	@Override
	public boolean isCachedFileValid(long nLink, byte[] hash) {
		FileSystemEntry entry = new FileSystemEntry(nLink);
		return SHA.verifyHash(entry.hash, hash);
	}
}
