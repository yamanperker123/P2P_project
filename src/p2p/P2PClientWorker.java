package p2p;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.Socket;
import p2p.gui.MainFrame;

public class P2PClientWorker extends Thread {

    private String peerIP;
    private int peerPort;
    private String localFilename;  // where to save the file
    private String remoteFilename; // the file we want from the peer
    private MainFrame mainFrame;
    private int chunkID; 

    private static final Object fileWriteLock = new Object();

    public P2PClientWorker(String peerIP, int peerPort, String localFilename, String remoteFilename,int chunkID,MainFrame mainFrame) {
        this.peerIP = peerIP;
        this.peerPort = peerPort;
        this.localFilename = localFilename;
        this.remoteFilename = remoteFilename;
        this.chunkID = chunkID;
        this.mainFrame = mainFrame;
    }

    @Override
    public void run() {
        try {
            // Notify GUI that we start a download
            if (mainFrame != null) {
                mainFrame.addDownloadRow(remoteFilename + " [Chunk " + chunkID + "]");
            }
            Socket socket = new Socket(peerIP, peerPort);
            DataOutputStream dOS = new DataOutputStream(socket.getOutputStream());
            DataInputStream dIS = new DataInputStream(socket.getInputStream());

            // Request specific chunk
            dOS.writeUTF("GET_CHUNK:" + remoteFilename + ":" + chunkID);
            dOS.flush();

            // read server response
            String response = dIS.readUTF();
            if (!response.startsWith("OK")) {
                System.out.println("Server error: " + response);
                socket.close();
                return;
            }

            // read chunk data
            int receivedChunkID = dIS.readInt();
            int chunkSize = dIS.readInt();
            byte[] buffer = new byte[chunkSize];
            dIS.readFully(buffer);

            // write chunk
            File outFile = new File(localFilename);
            if (!outFile.exists()) {
                synchronized (fileWriteLock) {
                    if (!outFile.exists()) { // Double-check within synchronized block
                        outFile.createNewFile();
                    }
                }
            }
            synchronized (fileWriteLock) {
                RandomAccessFile rAF = new RandomAccessFile(outFile, "rw");
                rAF.seek(chunkID * 256000);
                rAF.write(buffer);
                rAF.close();
            }

            // ack
            dOS.writeInt(receivedChunkID);

            // update progress
            if (mainFrame != null) {
                mainFrame.updateDownloadProgress(remoteFilename + " [Chunk " + chunkID + "]", 100);
            }

            socket.close();

            // done
            if (mainFrame != null) {
                mainFrame.markDownloadComplete(remoteFilename + " [Chunk " + chunkID + "]");
            }
            System.out.println("Finished downloading: " + remoteFilename + " [Chunk " + chunkID + "]");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

