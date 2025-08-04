package p2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer extends Thread {

    private int serverPort;
    private volatile boolean running = true;
    private ServerSocket welcomeSocket;
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public Peer(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try {
            welcomeSocket = new ServerSocket(serverPort);
            System.out.println("Peer server listening on port " + serverPort);

            while (running) {
                Socket connectionSocket = welcomeSocket.accept();
                System.out.println("Peer: Accepted connection from " + connectionSocket.getInetAddress() + ":" + connectionSocket.getPort());
                threadPool.execute(new P2PServerWorker(connectionSocket));
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        }
    }

    public void shutdownServer() {
        running = false;
        threadPool.shutdownNow();
        try {
            if (welcomeSocket != null && !welcomeSocket.isClosed()) {
                welcomeSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Peer server stopped.");
    }
}
