package p2p;

import p2p.gui.MainFrame;
import java.io.IOException;
import java.net.*;
import java.util.*;


public class DiscoveryService extends Thread {

    public static final String MULTICAST_ADDRESS = "230.255.0.1"; //address should be between 224.0.0.0 to 239.255.255.255
    public static final int DISCOVERY_PORT = 8888;

    private volatile boolean running = true;

    private final int serverPort;         
    private final MainFrame gui;         

    private MulticastSocket socket;
    private InetSocketAddress groupAddress;
    private NetworkInterface netIf;

    private final Set<String> peerAddresses = Collections.synchronizedSet(new HashSet<>());

    public DiscoveryService(int serverPort, MainFrame gui) {
        this.serverPort = serverPort;
        this.gui = gui;
    }

    @Override
    public void run() {
        try {
            groupAddress = new InetSocketAddress(MULTICAST_ADDRESS, DISCOVERY_PORT);
            netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

            socket = new MulticastSocket(DISCOVERY_PORT);
            socket.joinGroup(groupAddress, netIf);
            socket.setTimeToLive(1);

            System.out.println("[DiscoveryService] Joined " + MULTICAST_ADDRESS + ":" + DISCOVERY_PORT);

            // Periodic broadcast
            new Thread(() -> {
                while (running) {
                    broadcastPresence();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();

            byte[] buf = new byte[15000];
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (msg.startsWith("P2P_FILE_SHARING")) {
                        String[] parts = msg.split(";");
                        if (parts.length == 3) {
                            String peerIP = parts[1].trim();
                            int peerPort = Integer.parseInt(parts[2].trim());
                            String localIP = InetAddress.getLocalHost().getHostAddress();

                            if (!(peerIP.equals(localIP) && peerPort == this.serverPort)) {
                                String peerInfo = peerIP + ":" + peerPort;
                                if (!peerAddresses.contains(peerInfo)) {
                                    peerAddresses.add(peerInfo);
                                    System.out.println("[DiscoveryService] Discovered peer: " + peerInfo);

                                    // Add to GUI
                                    InetSocketAddress isa = new InetSocketAddress(peerIP, peerPort);
                                    gui.addDiscoveredPeer(isa);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[DiscoveryService] Error: " + e.getMessage());
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[DiscoveryService] Invalid port: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[DiscoveryService] Failed to start: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void broadcastPresence() {
        try {
            String localIP = InetAddress.getLocalHost().getHostAddress();
            String message = "P2P_FILE_SHARING;" + localIP + ";" + serverPort;
            byte[] data = message.getBytes();

            DatagramPacket packet = new DatagramPacket(data, data.length,
                groupAddress.getAddress(), groupAddress.getPort());
            socket.send(packet);
            System.out.println("[DiscoveryService] Broadcasting presence: " + message);
        } catch (IOException e) {
            System.err.println("[DiscoveryService] Broadcast error: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.leaveGroup(groupAddress, netIf);
                socket.close();
                System.out.println("[DiscoveryService] Multicast socket closed.");
            } catch (IOException e) {
                System.err.println("[DiscoveryService] Error closing: " + e.getMessage());
            }
        }
    }

    public Set<String> getPeerAddresses() {
        return peerAddresses;
    }
}
