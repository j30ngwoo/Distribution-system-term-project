import java.util.ArrayList;

class FileInfo {
    String name;
    String owner;
    Integer logicalClock;
    FileInfo(String fileName, String fileOwner, Integer logicalClock){
        this.name = fileName;
        this.logicalClock = logicalClock;
    }
} // TODO: file info setting

class FileUpdate {
    public static final int CREATED = 1;
    public static final int DELETED = 2;
    public static final int MODIFIED = 3;
}

class EventID {
    public static final int FILESYNC_FILECREATED = 1;
    public static final int FILESYNC_FILEDELETED = 2;
    public static final int FILESYNC_FILEMODIFIED = 3;

    public static final int FILESYNC_PUSH_REQUEST = 21;
    public static final int FILESYNC_PUSH_REJECT = 22;
    public static final int FILESYNC_FILE_DELETE_REQUEST = 23;
    public static final int FILESYNC_FILE_DELETE_REJECT = 24;
    public static final int FILESYNC_FILE_DELETE_NOT_EXIST = 25;
}

public class Utils {
    public static int findLogicalClock(String filename, ArrayList<FileInfo> fileList) {
        for (int i = 0; i < fileList.size(); i++)
            if (fileList.get(i).name.equals(filename))
                return (fileList.get(i).logicalClock);
        return (-1);
    }

    public static int increaseLogicalClock(String filename, ArrayList<FileInfo> fileList) {
        for (int i = 0; i < fileList.size(); i++){
            if (fileList.get(i).name.equals(filename)){
                fileList.get(i).logicalClock++;
                return (fileList.get(i).logicalClock);
            }
        }
        return (-1);
    }

    public static void setLogicalClock(String filename, ArrayList<FileInfo> fileList, Integer LC) {
        for (int i = 0; i < fileList.size(); i++){
            if (fileList.get(i).name.equals(filename)){
                fileList.get(i).logicalClock = LC;
                return;
            }
        }
    }

    public static void deleteFileFromList(String filename, ArrayList<FileInfo> fileList) {
        for (int i = 0; i < fileList.size(); i++)
            if (fileList.get(i).name.equals(filename))
                fileList.remove(i);
    }

}