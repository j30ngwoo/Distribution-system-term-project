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
}

public class Utils {
    public static int findLogicalClock(String filename, ArrayList<FileInfo> fileList) {
        for (int i = 0; i < fileList.size(); i++)
            if (fileList.get(i).name.equals(filename))
                return (fileList.get(i).logicalClock);
        return (-1);
    }
}