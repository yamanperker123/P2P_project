package p2p;

public class FileMetadata {
    private String fileName;
    private long fileSize;
    private String fullPath;  // Absolute path 

    public FileMetadata(String fileName, long fileSize, String fullPath) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fullPath = fullPath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFullPath() {
        return fullPath;
    }
}
