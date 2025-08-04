package p2p;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Random;

public class P2PServerWorker implements Runnable {

    private Socket socket;

    public P2PServerWorker(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream dIS = new DataInputStream(socket.getInputStream());
             DataOutputStream dOS = new DataOutputStream(socket.getOutputStream())) {

            
            String command = dIS.readUTF();  
            System.out.println("P2PServerWorker: Received command: " + command + " from " + socket.getInetAddress() + ":" + socket.getPort());

            if (command.equals("GET_FILE_LIST")) {
              
                List<FileMetadata> localFiles = GlobalConfig.getLocalSharedFiles();
                dOS.writeInt(localFiles.size());
                
                for (FileMetadata fm : localFiles) {
                    dOS.writeUTF(fm.getFileName());
                    dOS.writeLong(fm.getFileSize());
                }
                dOS.flush();

                socket.close();
                System.out.println("P2PServerWorker: Sent file list, closing connection.");

            } else if (command.startsWith("GET_FILE:")) {
                // GET_FILE
                String requestedName = command.substring("GET_FILE:".length()).trim();
                FileMetadata found = null;
                for (FileMetadata fm : GlobalConfig.getLocalSharedFiles()) {
                    if (fm.getFileName().equalsIgnoreCase(requestedName)) {
                        found = fm;
                        break;
                    }
                }
                if (found == null) {
                    dOS.writeUTF("ERROR:FILE_NOT_FOUND");
                    socket.close();
                    System.out.println("P2PServerWorker: File not found: " + requestedName);
                    return;
                }

                // file
                File file = new File(found.getFullPath());
                if (!file.exists()) {
                    dOS.writeUTF("ERROR:FILE_NOT_FOUND");
                    socket.close();
                    System.out.println("P2PServerWorker: File not found on disk: " + requestedName);
                    return;
                }
                // Send an OK response 
                dOS.writeUTF("OK");
                dOS.flush();

                
                RandomAccessFile rAF = new RandomAccessFile(file, "r");
                int length = (int) file.length();

                int chunkCount = (int) Math.ceil(length / 256000.0);
                int[] checkArray = new int[chunkCount];
                Random random = new Random();
                int loop = 0;

                // send total length
                dOS.writeInt(length);
                dOS.flush();

                while (loop < chunkCount) {
                    int i = random.nextInt(chunkCount);
                    if (checkArray[i] == 0) {
                        // Move file pointer to the start of the chunk
                        rAF.seek(i * 256000);
                        byte[] toSend = new byte[256000];
                        int read = rAF.read(toSend);
                        // send chunk ID, chunk size, chunk data
                        dOS.writeInt(i); // send chunk no
                        dOS.writeInt(read); // send read length
                        dOS.write(toSend, 0, read); // send data
                        dOS.flush();

                        // read ack
                        int ACK = dIS.readInt();
                        if (i == ACK) {
                            checkArray[i] = 1;
                            loop++;
                            System.out.println("P2PServerWorker: Chunk " + i + " acknowledged by client.");
                        }
                    }
                }
                System.out.println("P2PServerWorker: Sent all chunks to " + socket.getInetAddress().getHostAddress() + "...");
                rAF.close();
                dOS.writeInt(-1);
                dOS.flush();
                socket.close();

            } else if (command.startsWith("GET_CHUNK:")) {
                //GET_CHUNK
                String[] parts = command.split(":");
                if (parts.length == 3) {
                    String requestedName = parts[1].trim();
                    int requestedChunkID = Integer.parseInt(parts[2].trim());

                    FileMetadata found = null;
                    for (FileMetadata fm : GlobalConfig.getLocalSharedFiles()) {
                        if (fm.getFileName().equalsIgnoreCase(requestedName)) {
                            found = fm;
                            break;
                        }
                    }
                    if (found == null) {
                        dOS.writeUTF("ERROR:FILE_NOT_FOUND");
                        socket.close();
                        System.out.println("P2PServerWorker: File not found: " + requestedName);
                        return;
                    }

                    File file = new File(found.getFullPath());
                    if (!file.exists()) {
                        dOS.writeUTF("ERROR:FILE_NOT_FOUND");
                        socket.close();
                        System.out.println("P2PServerWorker: File not found on disk: " + requestedName);
                        return;
                    }

                    // Send an OK response so client 
                    dOS.writeUTF("OK");
                    dOS.flush();

                    RandomAccessFile rAF = new RandomAccessFile(file, "r");
                    rAF.seek(requestedChunkID * 256000);
                    byte[] toSend = new byte[256000];
                    int read = rAF.read(toSend);
                    rAF.close();

                    // send chunk ID, chunk size, chunk data
                    dOS.writeInt(requestedChunkID);
                    dOS.writeInt(read);
                    dOS.write(toSend, 0, read);
                    dOS.flush();

                    // read ack
                    int ACK = dIS.readInt();
                    if (ACK == requestedChunkID) {
                        System.out.println("P2PServerWorker: Chunk " + requestedChunkID + " acknowledged by client.");
                    }

                    socket.close();
                    System.out.println("P2PServerWorker: Sent chunk " + requestedChunkID + " of file: " + requestedName);

                } else {
                    dOS.writeUTF("ERROR:INVALID_COMMAND_FORMAT");
                    socket.close();
                    System.out.println("P2PServerWorker: Invalid GET_CHUNK command format.");
                }

            } else if (command.startsWith("GET_FILE_METADATA:")) {
                //GET_FILE_METADATA
                String requestedName = command.substring("GET_FILE_METADATA:".length()).trim();
                FileMetadata found = null;
                for (FileMetadata fm : GlobalConfig.getLocalSharedFiles()) {
                    if (fm.getFileName().equalsIgnoreCase(requestedName)) {
                        found = fm;
                        break;
                    }
                }
                if (found == null) {
                    dOS.writeUTF("ERROR:FILE_NOT_FOUND");
                    socket.close();
                    System.out.println("P2PServerWorker: File metadata not found: " + requestedName);
                    return;
                }

                // Send file metadata
                dOS.writeUTF("FILE_METADATA:" + found.getFileName() + ":" + found.getFileSize());
                dOS.flush();
                socket.close();
                System.out.println("P2PServerWorker: Sent metadata for file: " + requestedName);
            } else {
                dOS.writeUTF("ERROR:UNKNOWN_COMMAND");
                socket.close();
                System.out.println("P2PServerWorker: Unknown command received: " + command);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

