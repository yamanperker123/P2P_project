package p2p.gui;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
   //Default ports
        int serverPort = 6789;      // TCP port for Peer
        int discoveryPort = 9876;   // UDP port for DiscoveryService

        
        if (args.length > 0) {
            serverPort = Integer.parseInt(args[0]);  
        }
        if (args.length > 1) {
            discoveryPort = Integer.parseInt(args[1]); 
        }

        final int finalServerPort = serverPort;
        final int finalDiscoveryPort = discoveryPort;

        // Launch the GUI
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(finalServerPort, finalDiscoveryPort);
            frame.setVisible(true);
        });
    }
}
