package p2p;

import java.util.ArrayList;
import java.util.List;

public class GlobalConfig {

    // The root folder that we share (set via GUI)
    private static String rootSharedFolder = "";

    // Our list of local files in that folder
    private static final List<FileMetadata> localSharedFiles = new ArrayList<>();

    public static String getRootSharedFolder() {
        return rootSharedFolder;
    }

    public static void setRootSharedFolder(String folder) {
        rootSharedFolder = folder;
    }

    public static List<FileMetadata> getLocalSharedFiles() {
        return localSharedFiles;
    }

    public static void clearLocalSharedFiles() {
        localSharedFiles.clear();
    }

    public static void addLocalSharedFile(FileMetadata meta) {
        localSharedFiles.add(meta);
    }
}
