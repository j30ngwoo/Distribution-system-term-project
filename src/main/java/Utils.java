import java.util.ArrayList;

class SyncFileInfo {
    String name;
    Integer logicalClock;
    SyncFileInfo(String fileName, Integer logicalClock){
        this.name = fileName;
        this.logicalClock = logicalClock;
    }
}

class ShareFileInfo {
    String name;
    ArrayList<String> sharedClients = new ArrayList<>();
    ShareFileInfo(String fileName, String sharedClient){
        this.name = fileName;
        this.sharedClients.add(sharedClient);
    }
}

class EventID {
    public static final int FILESYNC_FILECREATED = 1;
    public static final int FILESYNC_FILEDELETED = 2;
    public static final int FILESYNC_FILEMODIFIED = 3;

    public static final int FILESYNC_PUSH_ACCEPT = 21;
    public static final int FILESYNC_PUSH_REJECT = 22;
    public static final int FILESYNC_FILE_DELETE_ACCEPT = 23;
    public static final int FILESYNC_FILE_DELETE_REJECT = 24;
    public static final int FILESYNC_FILE_DELETE_NOT_EXIST = 25;

    public static final int FILESHARE_REQUEST = 31;
    public static final int FILESHARE_NEWFILE_ACCEPT = 33;
    public static final int FILESHARE_TARGETCLIENT_NOT_EXIST = 34;
    public static final int FILESHARE_TARGETCLIENT_ALREADY_SHARE_THISFILE = 35;
    public static final int FILESHARE_CONFLICT_OCCURED = 36;
    public static final int FILESHARE_NEWCLIENT_ACCEPT = 37;
    public static final int FILESHARE_NEWFILE_RECEIVED = 38;
    public static final int FILESHARE_FILE_CASTED = 39;

    public static final int FILESHARESYNC_DELETE_REQUEST = 41;
    public static final int FILESHARESYNC_MODIFY_REQUEST = 42;
    public static final int FILESHARE_TARGETFILE_NOT_SHARED = 43;
}

public class Utils {
    public static int findLogicalClock(String filename, ArrayList<SyncFileInfo> fileList) {
        for (SyncFileInfo syncFileInfo : fileList)
            if (syncFileInfo.name.equals(filename))
                return (syncFileInfo.logicalClock);
        return (-1);
    }

    public static int increaseLogicalClock(String filename, ArrayList<SyncFileInfo> fileList) {
        for (SyncFileInfo syncFileInfo : fileList) {
            if (syncFileInfo.name.equals(filename)) {
                syncFileInfo.logicalClock++;
                return (syncFileInfo.logicalClock);
            }
        }
        return (-1);
    }

    public static void setLogicalClock(String filename, ArrayList<SyncFileInfo> fileList, Integer LC) {
        for (SyncFileInfo syncFileInfo : fileList) {
            if (syncFileInfo.name.equals(filename)) {
                syncFileInfo.logicalClock = LC;
                return;
            }
        }
    }

    public static void deleteFileFromList(String filename, ArrayList<SyncFileInfo> fileList) {
        for (int i = 0; i < fileList.size(); i++) {
            if (fileList.get(i).name.equals(filename)) {
                fileList.remove(i);
                break;
            }
        }
    }

    public static int findFileFromList(String fileName, ArrayList<ShareFileInfo> fileList) {
        for (int i = 0; i < fileList.size(); i++)
            if (fileList.get(i).name.equals(fileName))
                return (i);
        return (-1);
    }

    public static void addClientToList(String fileName, ArrayList<ShareFileInfo> fileList, String clientName) {
        for (ShareFileInfo shareFileInfo : fileList)
            if (shareFileInfo.name.equals(fileName))
                shareFileInfo.sharedClients.add(clientName);
    }

    public static int findUserInStringArray(String userName, ArrayList<String> clientList){
        for(int i = 0; i < clientList.size(); i++)
            if (clientList.get(i).equals(userName))
                return (i);
        return (-1);
    }
}