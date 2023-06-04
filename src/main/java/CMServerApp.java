import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.ArrayList;
import java.util.List;

public class CMServerApp {
    private CMServerStub m_serverStub;
    private CMServerEventHandler m_eventHandler;
    public String strFilePath = ".\\server-file-path";
    private boolean m_bRun;
    public static ArrayList<FileInfo> serverFileList = new ArrayList<>();

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
        List<String> localAddressList = null;
        localAddressList = CMCommManager.getLocalIPList();
        if (localAddressList == null) {
            System.err.println("Local address not found!");
            return;
        }

        System.out.println("my current address: " + localAddressList.get(0).toString());
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

        for (int i = 0; i < dirs.length; i++) {
            if (dirs[i].isDirectory()){
                File[] listFiles = (new File(dirs[i].getPath())).listFiles();
                for (int j = 0; j < listFiles.length; j++) {
                    System.out.print("New file detected: \'" + listFiles[j] + "\' - adding to file list\n");
                    serverFileList.add(new FileInfo(listFiles[j].getName(), getOwnerName(listFiles[j]), 1));
                    //System.out.print("test: " + listFiles[j].getName() + " " + getOwnerName(listFiles[j]) + "\n");
                }

            } else {
                System.out.print("New file detected: \'" + dirs[i] + "\' - adding to file list\n");
                serverFileList.add(new FileInfo(dirs[i].getName(), null, 1));
            }
        }
    } //TODO: initialize file list

    private String getOwnerName(File file){
        String[] strFile = file.toString().split("\\\\");
        return (strFile[strFile.length - 2]);
    }

    public void startServer() {
        m_serverStub.addBlockDatagramChannel(1);
        System.out.println("Server application starts.");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String strInput = null;
        int nCommand = -1;

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
                case 0:
                    printAllMenus();
                    break;
                case 999:
                    terminateCM();
                    return;
                case 1:
                    test();
                default:
                    System.err.println("Unknown command.");
                    break;
            }
        }

        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void test()
    {
        //CMServerEventHandler.processSyncFileCreated(1, "2", "1");
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