package p2p.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

import p2p.DiscoveryService;
import p2p.FileMetadata;
import p2p.GlobalConfig;
import p2p.P2PClientWorker;
import p2p.Peer;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;


public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    // Networking
    private int serverPort;              
    private Peer peer;
    private DiscoveryService discoveryService;

    // Discovered peers
    private List<InetSocketAddress> discoveredPeers = new ArrayList<>();
    private DefaultListModel<String> discoveredPeersModel;
    private JList<String> discoveredPeersList;

    // Root/dest folder
    private JTextField rootFolderField;
    private JButton setRootButton;
    private JTextField destinationFolderField;
    private JButton setDestinationButton;

    // Downloading files
    private DefaultTableModel downloadingTableModel;
    private JTable downloadingFilesTable;

    // Found files
    private DefaultTableModel foundFilesTableModel;
    private JTable foundFilesTable;

    // Buttons
    private JButton searchButton;
    private JButton downloadButton;  

    // Menu items
    private JMenuItem connectItem;
    private JMenuItem disconnectItem;
    private JMenuItem exitItem;
    private JMenuItem aboutItem;

    // Destination folder path
    private String destinationFolder = "C:\\P2PDownloads";

    // A new "Download Exclusion" for file masks
    private DefaultListModel<String> downloadExclusionMasksModel;
    private JList<String> downloadExclusionMasksList;
    private JTextField exclusionMaskField;
    private JButton addMaskButton, removeMaskButton;

    public MainFrame(int serverPort, int discoveryPort) {
        super("P2P File Sharing");
        this.serverPort = serverPort;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 700);
        setLocationRelativeTo(null);

        // ====== Menu Bar ======
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Files");
        connectItem = new JMenuItem("Connect");
        disconnectItem = new JMenuItem("Disconnect");
        exitItem = new JMenuItem("Exit");
        fileMenu.add(connectItem);
        fileMenu.add(disconnectItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        aboutItem = new JMenuItem("About");
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        // ====== Main Panel ======
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        getContentPane().add(mainPanel);

        // Root folder
        JPanel rootPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rootPanel.setBorder(BorderFactory.createTitledBorder("Root of the P2P shared folder"));
        rootFolderField = new JTextField("C:\\MySharedFolder", 30);
        setRootButton = new JButton("Set");
        rootPanel.add(rootFolderField);
        rootPanel.add(setRootButton);
        mainPanel.add(rootPanel);

        // Destination folder
        JPanel destPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        destPanel.setBorder(BorderFactory.createTitledBorder("Destination folder"));
        destinationFolderField = new JTextField(destinationFolder, 30);
        setDestinationButton = new JButton("Set");
        destPanel.add(destinationFolderField);
        destPanel.add(setDestinationButton);
        mainPanel.add(destPanel);

        // Discovered Peers panel
        JPanel peersPanel = new JPanel(new BorderLayout());
        peersPanel.setBorder(BorderFactory.createTitledBorder("Discovered Peers"));
        discoveredPeersModel = new DefaultListModel<>();
        discoveredPeersList = new JList<>(discoveredPeersModel);
        JScrollPane peersScroll = new JScrollPane(discoveredPeersList);
        peersPanel.add(peersScroll, BorderLayout.CENTER);
        mainPanel.add(peersPanel);

        // "Download Exclusion" panel for file masks
        JPanel exclusionPanel = new JPanel();
        exclusionPanel.setLayout(new BoxLayout(exclusionPanel, BoxLayout.Y_AXIS));
        exclusionPanel.setBorder(new TitledBorder("Exclude from Download"));

        // Model and list
        downloadExclusionMasksModel = new DefaultListModel<>();
        downloadExclusionMasksList = new JList<>(downloadExclusionMasksModel);
        JScrollPane exclScroll = new JScrollPane(downloadExclusionMasksList);
        exclScroll.setPreferredSize(new Dimension(200, 60));

        // Add/Remove mask
        exclusionMaskField = new JTextField(15);
        addMaskButton = new JButton("Add");
        removeMaskButton = new JButton("Remove");

        // Sub-panel for adding
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.add(new JLabel("File Mask:"));
        addPanel.add(exclusionMaskField);
        addPanel.add(addMaskButton);
        addPanel.add(removeMaskButton);

        exclusionPanel.add(exclScroll);
        exclusionPanel.add(addPanel);
        mainPanel.add(exclusionPanel);

        // Downloading files
        JPanel downloadingPanel = new JPanel(new BorderLayout());
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));
        downloadingTableModel = new DefaultTableModel(
            new Object[]{"File Name", "Progress", "Status"}, 0
        );
        downloadingFilesTable = new JTable(downloadingTableModel);
        JScrollPane downloadingScroll = new JScrollPane(downloadingFilesTable);
        downloadingPanel.add(downloadingScroll, BorderLayout.CENTER);
        mainPanel.add(downloadingPanel);

        // Found files
        JPanel foundPanel = new JPanel(new BorderLayout());
        foundPanel.setBorder(BorderFactory.createTitledBorder("Found files"));
        foundFilesTableModel = new DefaultTableModel(
            new Object[]{"File Name", "Size", "Source Peer"}, 0
        );
        foundFilesTable = new JTable(foundFilesTableModel);
        JScrollPane foundScroll = new JScrollPane(foundFilesTable);
        foundPanel.add(foundScroll, BorderLayout.CENTER);
        mainPanel.add(foundPanel);

        // Bottom panel: Search + Download
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchButton = new JButton("Search");
        bottomPanel.add(searchButton);

        downloadButton = new JButton("Download");
        bottomPanel.add(downloadButton);
        mainPanel.add(bottomPanel);

        // ====== Menu item actions ======
        connectItem.addActionListener(e -> onConnect());
        disconnectItem.addActionListener(e -> onDisconnect());
        exitItem.addActionListener(e -> System.exit(0));
        aboutItem.addActionListener(e ->
            JOptionPane.showMessageDialog(this,
                "Developer: Yaman Perker \n  \t No: 20220702120",
                "About", JOptionPane.INFORMATION_MESSAGE)
        );

        // Root folder "Set"
        setRootButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                rootFolderField.setText(chooser.getSelectedFile().getAbsolutePath());
                scanSharedFolder();
            }
        });

        // Destination folder "Set"
        setDestinationButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                destinationFolderField.setText(chooser.getSelectedFile().getAbsolutePath());
                destinationFolder = chooser.getSelectedFile().getAbsolutePath();
            }
        });

        // Add/Remove file mask for download
        addMaskButton.addActionListener(e -> {
            String mask = exclusionMaskField.getText().trim();
            if (!mask.isEmpty()) {
                // e.g. *.pdf or testfile.*
                if (!downloadExclusionMasksModel.contains(mask)) {
                    downloadExclusionMasksModel.addElement(mask);
                }
                exclusionMaskField.setText("");
            }
        });
        removeMaskButton.addActionListener(e -> {
            int sel = downloadExclusionMasksList.getSelectedIndex();
            if (sel >= 0) {
                downloadExclusionMasksModel.remove(sel);
            }
        });

        // Search gather file lists from discovered peers
        searchButton.addActionListener(e -> doSearchFileLists());

        // Download selected file from Found files, respecting exclusions
        downloadButton.addActionListener(e -> onDownloadSelected());

        // Show
        setVisible(true);
        scanSharedFolder();
    }

    
    // Connect/Disconnect
    private void onConnect() {
        startPeerServer();
        startDiscovery();
    }

    private void onDisconnect() {
        stopDiscovery();
        stopPeerServer();
    }

    private void startPeerServer() {
        if (peer == null) {
            peer = new Peer(serverPort);
            peer.start();
            JOptionPane.showMessageDialog(this,
                "Peer server started on port " + serverPort + ".",
                "Server Started",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void stopPeerServer() {
        if (peer != null) {
            peer.shutdownServer();
            peer = null;
            JOptionPane.showMessageDialog(this,
                "Peer server stopped.",
                "Server Stopped",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void startDiscovery() {
        if (discoveryService == null) {
            discoveryService = new DiscoveryService(serverPort, this);
            discoveryService.start();
            JOptionPane.showMessageDialog(this,
                "Multicast discovery started on 230.255.0.1:8888",
                "Discovery Started",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void stopDiscovery() {
        if (discoveryService != null) {
            discoveryService.shutdown();
            discoveryService = null;
            JOptionPane.showMessageDialog(this,
                "Discovery service stopped.",
                "Discovery Stopped",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

   
    // Called by DiscoveryService
    public void addDiscoveredPeer(InetSocketAddress addr) {
        SwingUtilities.invokeLater(() -> {
            if (!discoveredPeers.contains(addr)) {
                discoveredPeers.add(addr);
                discoveredPeersModel.addElement(addr.toString());
            }
        });
    }

    public void addDiscoveredPeer(String peerInfo) {
        SwingUtilities.invokeLater(() -> {
            discoveredPeersModel.addElement(peerInfo);
        });
    }

 
    // Download table updates
 
    public void addDownloadRow(String filename) {
        SwingUtilities.invokeLater(() -> {
            Object[] rowData = { filename, "0%", "Downloading" };
            downloadingTableModel.addRow(rowData);
        });
    }

    public void updateDownloadProgress(String filename, int progress) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < downloadingTableModel.getRowCount(); i++) {
                String existingFile = (String) downloadingTableModel.getValueAt(i, 0);
                if (existingFile.equals(filename)) {
                    downloadingTableModel.setValueAt(progress + "%", i, 1);
                    if (progress >= 100) {
                        downloadingTableModel.setValueAt("Complete", i, 2);
                    }
                    break;
                }
            }
        });
    }

    public void markDownloadComplete(String filename) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < downloadingTableModel.getRowCount(); i++) {
                String existingFile = (String) downloadingTableModel.getValueAt(i, 0);
                if (existingFile.equals(filename)) {
                    downloadingTableModel.setValueAt("100%", i, 1);
                    downloadingTableModel.setValueAt("Complete", i, 2);
                    break;
                }
            }
        });
    }

   
    // Local folder scanning
  
    private void scanSharedFolder() {
        GlobalConfig.clearLocalSharedFiles();
        String rootPath = rootFolderField.getText().trim();
        GlobalConfig.setRootSharedFolder(rootPath);
        File rootDir = new File(rootPath);
        if (rootDir.exists() && rootDir.isDirectory()) {
            scanFolderRecursive(rootDir);
        }
        System.out.println("Scanned " + GlobalConfig.getLocalSharedFiles().size() + " files in local shared folder: " + rootPath);
    }

    private void scanFolderRecursive(File dir) {
        if (dir == null || !dir.exists()) return;
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                // no subfolder exclusion
                scanFolderRecursive(f);
            } else {
                
                FileMetadata meta = new FileMetadata(f.getName(), f.length(), f.getAbsolutePath());
                GlobalConfig.addLocalSharedFile(meta);
            }
        }
    }

   
    // Searching from discovered peers
    private void doSearchFileLists() {
        foundFilesTableModel.setRowCount(0);
        for (InetSocketAddress isa : discoveredPeers) {
            try (Socket sock = new Socket(isa.getAddress(), isa.getPort());
                 DataOutputStream dOS = new DataOutputStream(sock.getOutputStream());
                 DataInputStream dIS = new DataInputStream(sock.getInputStream())) {

                dOS.writeUTF("GET_FILE_LIST");
                dOS.flush();

                int count = dIS.readInt();
                for (int i = 0; i < count; i++) {
                    String fName = dIS.readUTF();
                    long fSize   = dIS.readLong();
                    foundFilesTableModel.addRow(new Object[]{ fName, fSize, isa.toString() });
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        JOptionPane.showMessageDialog(this, "Search complete! Found files have been listed.");
    }

 
    // onDownloadSelected (with "downloadExclusion" logic)

    private void onDownloadSelected() {
        int row = foundFilesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select a file in 'Found files' before clicking Download.",
                "No file selected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        String fileName = (String) foundFilesTableModel.getValueAt(row, 0);
        String peerStr  = (String) foundFilesTableModel.getValueAt(row, 2);

        // Check if this file is excluded from download
        if (isDownloadExcluded(fileName)) {
            JOptionPane.showMessageDialog(this,
                "This file '" + fileName + "' is excluded from download.",
                "Excluded from Download",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String trimmed = peerStr.replace("/", "");
        String[] parts = trimmed.split(":");
        if (parts.length == 2) {
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);

            String localPath = destinationFolder + File.separator + fileName;
            // Single-chunk approach
            P2PClientWorker worker = new P2PClientWorker(ip, port, localPath, fileName, 0, this);
            worker.start();
            JOptionPane.showMessageDialog(this, "Download started for " + fileName);
        }
    }

    private boolean isDownloadExcluded(String fileName) {
        for (int i = 0; i < downloadExclusionMasksModel.size(); i++) {
            String mask = downloadExclusionMasksModel.get(i);
            String regex = mask.replace("*", ".*");
            if (fileName.matches(regex)) {
                return true;
            }
        }
        return false;
    }
}
