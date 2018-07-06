package pt.inescid.safecloudfs.recovery.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import pt.inescid.safecloudfs.SafeCloudFS;
import pt.inescid.safecloudfs.directoryService.DirectoryService;
import pt.inescid.safecloudfs.recovery.FileRecover;
import pt.inescid.safecloudfs.recovery.SafeCloudFSLog;
import pt.inescid.safecloudfs.recovery.SafeCloudFSLogEntry;

public class SafeCloudFSLogViewer {

	public JFrame frame;
	private JTable tableLog;

	private TableModel tableLogModel;

	private JTree treeFileSystem;

	private DefaultMutableTreeNode root;
	private DefaultTreeModel model;

	private DirectoryService directoryService;

	private SafeCloudFSLog safeCloudLog;



	public void setLog(SafeCloudFSLog safeCloudLog) {
		this.safeCloudLog = safeCloudLog;
	}

	/**
	 * Create the application.
	 */
	public SafeCloudFSLogViewer(DirectoryService directoryService, SafeCloudFSLog safeCloudLog) {
		this.directoryService = directoryService;
		this.safeCloudLog = safeCloudLog;
		initialize();

	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane.setResizeWeight(.5d);

		frame.getContentPane().add(splitPane, BorderLayout.CENTER);

		JPanel panelTop = new JPanel();
		splitPane.setLeftComponent(panelTop);
		panelTop.setLayout(new GridLayout(0, 1, 0, 0));

		root = new DefaultMutableTreeNode("/");

		model = new DefaultTreeModel(root);

		treeFileSystem = new JTree(model);
		panelTop.add(treeFileSystem);

		JPanel panelBottom = new JPanel();
		splitPane.setRightComponent(panelBottom);
		panelBottom.setLayout(new GridLayout(0, 1, 0, 0));

		JScrollPane scrollPane = new JScrollPane();
		panelBottom.add(scrollPane);

		tableLog = new JTable();
		scrollPane.setViewportView(tableLog);
		tableLog.setFillsViewportHeight(true);

		tableLogModel = new DefaultTableModel(new Object[0][0], new Object[] {"id", "timestamp", "userId", "filePath",
				"fileNLink", "version", "operation", "clientIpAddress", "mode" });
		tableLog.setModel(tableLogModel);

		tableLog.setAutoCreateRowSorter(true); //a generic sorter

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItemUndo = new JMenuItem("Undo to this version");
		menuItemUndo.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {


				int logEntryId = Integer.parseInt(tableLog.getModel().getValueAt(tableLog.getSelectedRow(), 0).toString());
				SafeCloudFSLogEntry logEntry = SafeCloudFS.safeCloudFSLog.getLogEntryById(logEntryId);

				FileRecover.undoLogEntry(logEntry);

			}
		});

		popupMenu.add(menuItemUndo);

		tableLog.setComponentPopupMenu(popupMenu);


		MouseListener ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {

				// int selRow = treeFileSystem.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = treeFileSystem.getPathForLocation(e.getX(), e.getY());

				if (selPath != null) {
					treeFileSystem.setSelectionPath(selPath);
					treeFileSystem.scrollPathToVisible(selPath);

					String path = selPath.toString().substring(1, selPath.toString().length() - 1).replaceAll(", ", "/")
							.replaceAll("//", "/");
					userSelectedPath(path);
				}

			}

		};
		treeFileSystem.addMouseListener(ml);
	}

	private void userSelectedPath(String path) {
		((DefaultTableModel) tableLogModel).setRowCount(0);

		ArrayList<SafeCloudFSLogEntry> logEntries = this.safeCloudLog.getLogEntriesByPath(path);
		for (SafeCloudFSLogEntry entry : logEntries) {
			addLogEntry(entry);
		}


	}

	public void addLogEntry(SafeCloudFSLogEntry logEntry) {
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String ts = format1.format(logEntry.timestamp);

		String formattedId = String.format("%04d", logEntry.id);

		((DefaultTableModel) tableLogModel).addRow(new Object[] {formattedId, ts, logEntry.userId, logEntry.filePath,
				logEntry.fileNLink, logEntry.version, logEntry.operation, logEntry.clientIpAddress, logEntry.mode });
		updateTreeFileSystemNode();
	}

	private void updateTreeFileSystemNode() {

		root.removeAllChildren();
		readDirAndPopulateTree("/", root);

		model.reload(root);

	}

	private void readDirAndPopulateTree(String path, DefaultMutableTreeNode thisNode) {
		ArrayList<String> dirObjects = this.directoryService.readDir(path);
		for (String object : dirObjects) {

			String objectPath = "";
			if (path.equals("/")) {
				objectPath = path + object;
			} else {
				objectPath = path + "/" + object;
			}

			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(object);

			thisNode.add(newNode);

			if (directoryService.isDir(objectPath)) {

				readDirAndPopulateTree(objectPath, newNode);
			}

		}
	}

}
