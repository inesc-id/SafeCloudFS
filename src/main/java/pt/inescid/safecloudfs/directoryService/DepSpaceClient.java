package pt.inescid.safecloudfs.directoryService;

import java.util.ArrayList;
import java.util.Arrays;
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

public class DepSpaceClient implements DirectoryService {



	private DepSpaceAccessor accessor;

	public DepSpaceClient(String hostsFile) {

		String name = "Demo Space";

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
			DepTuple tuple = new FileSystemEntry("/", true, 0, 0, 0).toTuple();
			accessor.out(tuple);
			System.out.println(accessor.rd(tuple).toString());

			DepTuple template = DepTuple.createTuple(new Object[] { "nlink", "*" });

			if (accessor.rdAll(template, 0).isEmpty()) {
				DepTuple tupleNlink = DepTuple.createTuple(new Object[] { "nlink", "1" });
				accessor.out(tupleNlink);
			}

		} catch (DepSpaceException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
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

			Object[] pathParts = path.split("/");
			pathParts = appendToBeginOfArray(pathParts, "/");
			for (int i = 0; i < SafeCloudFSConstants.MAX_DEPTH; i++) {

				Object[] templateFields = new Object[SafeCloudFSConstants.MAX_DEPTH + FileSystemEntry.POS_FILE_PATH];
				System.arraycopy(pathParts, 0, templateFields, 0, pathParts.length);
				for (int j = 0; j < i; j++) {
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
		System.out.println("MKFile path=" + path);

		try {
			FileSystemEntry fse = new FileSystemEntry(path, false, 0, Calendar.getInstance().getTimeInMillis(),
					getNewNLink());
			DepTuple tuple = fse.toTuple();
			System.out.println("Tuple=" + tuple.toString());
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
		System.out.println("MKDIR = " + path);
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
			System.out.println("getNLink= " + result);
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
			System.out.println("getMode= " + result);
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
			System.out.println("Exists? (" + path + ")=" + result);
			return result;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean isDir(String path) {
		try {
			boolean result = new FileSystemEntry(path).isDir;
			System.out.println("isDir? (" + path + ")=" + result);
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

			System.out.println("getParent=(" + path + ")=" + parentPath);
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

			Object[] tupleTemplateFields = new Object[pathParts.length + FileSystemEntry.POS_FILE_PATH];

			System.arraycopy(pathParts, 0, tupleTemplateFields, 0, pathParts.length);

			tupleTemplateFields[tupleTemplateFields.length - 5] = "*";
			tupleTemplateFields[tupleTemplateFields.length - 4] = "*";
			tupleTemplateFields[tupleTemplateFields.length - 3] = "*";
			tupleTemplateFields[tupleTemplateFields.length - 2] = "*";
			tupleTemplateFields[tupleTemplateFields.length - 1] = "*";

			System.out.println("Template=" + Arrays.asList(tupleTemplateFields));

			DepTuple template = DepTuple.createTuple(tupleTemplateFields);

			ArrayList<DepTuple> children = null;
			try {
				children = new ArrayList<>(accessor.rdAll(template, 0));
			} catch (DepSpaceException e) {
				e.printStackTrace();
			}
			ArrayList<String> result = new ArrayList<>();
			for (DepTuple entry : children) {
				System.out.println("File in Folder = " + Arrays.asList(entry.getFields()).toString());
				result.add(entry.getFields()[entry.getFields().length - FileSystemEntry.POS_FILE_PATH].toString());

			}
			System.out.println("ReadDir " + path + " result = " + result.toString());

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
		private static final String FILE_SYSTEM_ENTRY_ID = "FILE_SYSTEM_ENTRY";

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

				Object[] nullValues = new Object[] { "*", "*", "*", "*", "*" };

				Object[] tupleFields = new Object[pathParts.length + nullValues.length];

				System.arraycopy(pathParts, 0, tupleFields, 0, pathParts.length);
				System.arraycopy(nullValues, 0, tupleFields, pathParts.length, nullValues.length);

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
				System.out.println("ToTuple");
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

				System.out.println("ToTuple\n" + Arrays.asList(tupleFields).toString());

				return DepTuple.createTuple(tupleFields);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

		}

		public void delete() {
			try {
				accessor.in(this.toTuple());
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
