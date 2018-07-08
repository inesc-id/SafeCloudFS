package pt.inescid.safecloudfs.recovery.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
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
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.BoxLayout;
import java.awt.FlowLayout;
import javax.swing.SpringLayout;

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
		frame.setBounds(100, 100, 710, 406);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane.setResizeWeight(.5d);

		frame.getContentPane().add(splitPane, BorderLayout.CENTER);

		JPanel panelTop = new JPanel();
		splitPane.setLeftComponent(panelTop);

		root = new DefaultMutableTreeNode("/");

		model = new DefaultTreeModel(root);
		SpringLayout sl_panelTop = new SpringLayout();
		panelTop.setLayout(sl_panelTop);

		JLabel lblFileSystemFiles = new JLabel("File system files");
		sl_panelTop.putConstraint(SpringLayout.NORTH, lblFileSystemFiles, 0, SpringLayout.NORTH, panelTop);
		sl_panelTop.putConstraint(SpringLayout.WEST, lblFileSystemFiles, 0, SpringLayout.WEST, panelTop);
		sl_panelTop.putConstraint(SpringLayout.EAST, lblFileSystemFiles, 0, SpringLayout.EAST, panelTop);
		lblFileSystemFiles.setVerticalAlignment(SwingConstants.TOP);
		lblFileSystemFiles.setFont(new Font("Lucida Grande", Font.PLAIN, 30));

		panelTop.add(lblFileSystemFiles);

		treeFileSystem = new JTree(model);
		sl_panelTop.putConstraint(SpringLayout.NORTH, treeFileSystem, 0, SpringLayout.SOUTH, lblFileSystemFiles);
		sl_panelTop.putConstraint(SpringLayout.WEST, treeFileSystem, 0, SpringLayout.WEST, lblFileSystemFiles);
		sl_panelTop.putConstraint(SpringLayout.SOUTH, treeFileSystem, 0, SpringLayout.SOUTH, panelTop);
		sl_panelTop.putConstraint(SpringLayout.EAST, treeFileSystem, 0, SpringLayout.EAST, lblFileSystemFiles);
		panelTop.add(treeFileSystem);

		JPanel panelBottom = new JPanel();
		splitPane.setRightComponent(panelBottom);
		SpringLayout sl_panelBottom = new SpringLayout();
		panelBottom.setLayout(sl_panelBottom);

		JLabel lblLogOfOperations = new JLabel("Log of operations");
		sl_panelBottom.putConstraint(SpringLayout.NORTH, lblLogOfOperations, 0, SpringLayout.NORTH, panelBottom);
		sl_panelBottom.putConstraint(SpringLayout.WEST, lblLogOfOperations, 0, SpringLayout.WEST, panelBottom);
//		sl_panelBottom.putConstraint(SpringLayout.SOUTH, lblLogOfOperations, 41, SpringLayout.NORTH, panelBottom);
		sl_panelBottom.putConstraint(SpringLayout.EAST, lblLogOfOperations, 0, SpringLayout.EAST, panelBottom);
		lblLogOfOperations.setVerticalAlignment(SwingConstants.TOP);
		lblLogOfOperations.setFont(new Font("Lucida Grande", Font.PLAIN, 30));
		panelBottom.add(lblLogOfOperations);

		JScrollPane scrollPane = new JScrollPane();
		sl_panelBottom.putConstraint(SpringLayout.NORTH, scrollPane, 0, SpringLayout.SOUTH, lblLogOfOperations);
		sl_panelBottom.putConstraint(SpringLayout.WEST, scrollPane, 0, SpringLayout.WEST, lblLogOfOperations);
		sl_panelBottom.putConstraint(SpringLayout.SOUTH, scrollPane, 0, SpringLayout.SOUTH, panelBottom);
		sl_panelBottom.putConstraint(SpringLayout.EAST, scrollPane, 0, SpringLayout.EAST, lblLogOfOperations);
		panelBottom.add(scrollPane);

		tableLog = new JTable();
		scrollPane.setViewportView(tableLog);
		tableLog.setFillsViewportHeight(true);

		tableLogModel = new DefaultTableModel(new Object[0][0], new Object[] { "id", "timestamp", "userId", "filePath",
				"fileNLink", "version", "operation", "clientIpAddress", "mode" });
		tableLog.setModel(tableLogModel);

		tableLog.setAutoCreateRowSorter(true); // a generic sorter

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItemUndo = new JMenuItem("Undo to this version");
		menuItemUndo.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {

				int logEntryId = Integer
						.parseInt(tableLog.getModel().getValueAt(tableLog.getSelectedRow(), 0).toString());
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

		((DefaultTableModel) tableLogModel).addRow(new Object[] { formattedId, ts, logEntry.userId, logEntry.filePath,
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
			if (!object.startsWith(".")) {
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

}
