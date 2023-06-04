import javax.swing.*;
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

class FileUpdate {
    public static final int CREATED = 1;
    public static final int DELETED = 2;
    public static final int MODIFIED = 3;
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
    public static final int FILESHARE_SYNC = 32;
    public static final int FILESHARE_NEWFILE_ACCEPT = 33;
    public static final int FILESHARE_TARGETCLIENT_NOT_EXIST = 34;
    public static final int FILESHARE_TARGETCLIENT_ALREADY_SHARE_THISFILE = 35;
    public static final int FILESHARE_CONFLICT_OCCURED = 36;
    public static final int FILESHARE_NEWCLIENT_ACCEPT = 37;


    public static final int FILESHARE_FILEPUSH = 41;
}

public class Utils {
    public static int findLogicalClock(String filename, ArrayList<SyncFileInfo> fileList) {
        for (int i = 0; i < fileList.size(); i++)
            if (fileList.get(i).name.equals(filename))
                return (fileList.get(i).logicalClock);
        return (-1);
    }

    public static int increaseLogicalClock(String filename, ArrayList<SyncFileInfo> fileList) {
        for (int i = 0; i < fileList.size(); i++){
            if (fileList.get(i).name.equals(filename)){
                fileList.get(i).logicalClock++;
                return (fileList.get(i).logicalClock);
            }
        }
        return (-1);
    }

    public static void setLogicalClock(String filename, ArrayList<SyncFileInfo> fileList, Integer LC) {
        for (int i = 0; i < fileList.size(); i++){
            if (fileList.get(i).name.equals(filename)){
                fileList.get(i).logicalClock = LC;
                return;
            }
        }
    }

    public static void deleteFileFromList(String filename, ArrayList<SyncFileInfo> fileList) {
        for (int i = 0; i < fileList.size(); i++)
            if (fileList.get(i).name.equals(filename))
                fileList.remove(i);
    }

    public static int findFileFromList(String fileName, ArrayList<ShareFileInfo> fileList) {
        for (int i = 0; i < fileList.size(); i++)
            if (fileList.get(i).name.equals(fileName))
                return (i);
        return (-1);
    }

    public static void addClientToList(String fileName, ArrayList<ShareFileInfo> fileList, String clientName) {
        for (int i = 0; i < fileList.size(); i++)
            if (fileList.get(i).name.equals(fileName))
                fileList.get(i).sharedClients.add(clientName);
    }

    public static int findUserInStringArray(String userName, ArrayList<String> clientList){
        for(int i = 0; i < clientList.size(); i++)
            if (clientList.get(i).equals(userName))
                return (i);
        return (-1);
    }
}