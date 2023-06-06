import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CMServerApp {
    public CMServerStub m_serverStub;
    private final CMServerEventHandler m_eventHandler;
    public String strFilePath = ".\\server-file-path";
    private boolean m_bRun;
    public static ArrayList<SyncFileInfo> serverSyncFileList = new ArrayList<>();


    public CMServerApp() {
        m_serverStub = new CMServerStub();
        m_eventHandler = new CMServerEventHandler(m_serverStub);
        m_bRun = true;
    }

    public CMServerStub getServerStub() {
        return m_serverStub;
    }

    public CMServerEventHandler getServerEventHandler() {
        return m_eventHandler;
    }

    public void startCM() {
        List<String> localAddressList;
        localAddressList = CMCommManager.getLocalIPList();
        if (localAddressList == null) {
            System.err.println("Local address not found!");
            return;
        }

        System.out.println("my current address: " + localAddressList.get(0));
        System.out.println("saved server address: " + m_serverStub.getServerAddress());
        System.out.println("saved server port: " + m_serverStub.getServerPort());

        boolean bRet = m_serverStub.startCM();
        if (!bRet) {
            System.err.println("CM initialization error!");
            return;
        }
        setFileList();
        startServer();
    }

    private void setFileList(){
        File serverDir = new File(strFilePath);
        File[] dirs = serverDir.listFiles();

        for (File dir : Objects.requireNonNull(dirs)) {
            if (dir.isDirectory()) {
                File[] listFiles = (new File(dir.getPath())).listFiles();
                for (File listFile : Objects.requireNonNull(listFiles)) {
                    System.out.print("New file detected: '" + listFile + "' - adding to file list\n");
                    serverSyncFileList.add(new SyncFileInfo(listFile.getName(), 1));
                }

            } else {
                System.out.print("New file detected: '" + dir + "' - adding to file list\n");
                serverSyncFileList.add(new SyncFileInfo(dir.getName(), 1));
            }
        }
    }

    public void startServer() {
        m_serverStub.addBlockDatagramChannel(1);
        System.out.println("Server application starts.");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String strInput;
        int nCommand;

        while (m_bRun) {
            System.out.println("Type \"0\" for menu.");
            try {
                strInput = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            try {
                nCommand = Integer.parseInt(strInput);
            } catch (NumberFormatException e) {
                System.out.println("Incorrect command number!");
                continue;
            }

            switch (nCommand) {
                case 0 -> printAllMenus();
                case 9 -> terminateCM();
                //case 1 -> test();
                default -> System.err.println("Unknown command.");
            }
        }

        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printAllMenus()
    {
        System.out.println("---------------------------------- Help");
        System.out.println("0: show all menus");
        System.out.println("---------------------------------- Stop");
        System.out.println("999: terminate CM");
        System.out.println("----------------------------------");
    }

    public void terminateCM()
    {
        m_serverStub.terminateCM();
        m_bRun = false;
    }
    public static void main(String[] args) {
        CMServerApp server = new CMServerApp();
        CMServerStub cmStub = server.getServerStub();
        cmStub.setAppEventHandler(server.getServerEventHandler());
        server.startCM();

        System.out.println("Server application is terminated.");
    }
}