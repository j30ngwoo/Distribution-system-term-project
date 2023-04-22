import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;

public class CMClientApp {
    private static CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;
    private boolean m_bRun;
    private String strFilePath = null;
    public static final String R = "\u001B[0m";
    public static final String G = "\u001B[32m";
    public static final String Y = "\u001B[33m";
    public static final String B = "\u001B[34m";
    public static final String M = "\u001B[35m";

    public CMClientApp() {
        m_clientStub = new CMClientStub();
        m_eventHandler = new CMClientEventHandler(m_clientStub);
        m_bRun = true;
    }

    public CMClientStub getClientStub() {
        return m_clientStub;
    }

    public CMClientEventHandler getClientEventHandler() {
        return m_eventHandler;
    }

    public void StartCM() {
        // get local address
        List<String> localAddressList = CMCommManager.getLocalIPList();
        if (localAddressList == null) {
            System.err.println("Local address not found!");
            return;
        }

        String strCurrentLocalAddress = localAddressList.get(0).toString();
        String strSavedServerAddress = null;
        int nSavedServerPort = -1;

        strSavedServerAddress = m_clientStub.getServerAddress();
        nSavedServerPort = m_clientStub.getServerPort();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(G + "# start CM");
        System.out.println(Y + "my current address: " + R + strCurrentLocalAddress);
        System.out.println(Y + "saved server address: " + R + strSavedServerAddress);
        System.out.println(Y + "saved server port: " + R + nSavedServerPort);

        boolean bRet = m_clientStub.startCM();
        if (!bRet) {
            System.err.println("CM initialization error!");
            return;
        }
        startClient();
    }

    public void startClient() {
        System.out.println(G + "client application starts." + R);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String strInput = null;
        // int nCommand = -1;

        String defaultFilePath = null;

        while (m_bRun) {
            System.out.println(G + "Type [menu] to view the menu." + R);

            try {
                strInput = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            switch (strInput) {
                case "menu":        // view menu
                    printAllMenus();
                    break;
                case "terminate":   // terminate CM
                    terminateCM();
                    break;
                case "connect":     // connect to default server
                    connectionDS();
                    break;
                case "disconnect":  // disconnect from default server
                    disconnectionDS();
                    break;
                case "login":       // synchronously login to default server, and set the file path
                    loginDS();
                    break;
                case "logout":      // logout from default server
                    logoutDS();
                    break;
                case "checkfilepath":
                    checkFilePath();
                    break;
                case "setfilepath":
                    setFilePath();
                    break;
                case "pushfile":
                    pushFile();
                    break;
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

    public void printAllMenus() {
        System.out.println(R + "---------------------------------- Help");
        System.out.println(G + "[menu]" + R + ": show all menus.");
        System.out.println("---------------------------------- Stop");
        System.out.println(G + "[terminate]" + R + ": terminate CM.");
        System.out.println("---------------------------------- Connection");
        System.out.println(G + "[connect]" + R + ": connect to default server.");
        System.out.println(G + "[disconnect]" + R + ": disconnect from default server.");
        System.out.println("---------------------------------- Login");
        System.out.println(G + "[login]" + R + ": login to default server.");
        System.out.println(G + "[logout]" + R + ": logout from default server.");
        System.out.println("---------------------------------- File Transfer");
        System.out.println(G + "[setfilepath]" + R + ": Set a new file path.");
        System.out.println(G + "[checkfilepath]" + R + ": Check the current file path.");
        System.out.println(G + "[pushfile]" + R + ": Transfer the file to the server.");
        System.out.println("----------------------------------");
    }

    public void terminateCM() {
        m_clientStub.terminateCM();
        m_bRun = false;
    }

    public void connectionDS() {
        System.out.println("# " + G + "connect to default server." + R);
        m_clientStub.connectToServer();
        System.out.println("======");
    }

    public void disconnectionDS() {
        System.out.println("# " + G + "disconnect from default server." + R);
        m_clientStub.disconnectFromServer();
        System.out.println("======");
    }

    public void loginDS() {
        String strUserName = null;
        String strPassword = null;

        System.out.println(R + "#" + G + " login to default server." + R);
        System.out.print(Y + "user name: " + R);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            strUserName = br.readLine();
            System.out.print(Y + "password: " + R);
            strPassword = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (m_clientStub.loginCM(strUserName, strPassword)) {
            System.out.println(G + "This client successfully logs in to the default server." + R);
            strFilePath = ".\\client-file-path-" + strUserName + "\\"; // initial set filepath
        } else {
            System.err.println("failed the login request!");
        }
        System.out.println(R + "======");
    }

    public void logoutDS() {
        if (m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState() != CMInfo.CM_CHAR) {
            System.err.println("Client is not logged in.");
            return;
        }
        System.out.println("#" + G + " logout from default server." + R);
        if (m_clientStub.logoutCM())
            System.out.println(G + "successfully sent the logout request." + R);
        else
            System.err.println("failed the logout request!");
        System.out.println("======");
    }

    public void setFilePath() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("#" + G + "set file path.");
        if (strFilePath != null)
            System.out.println("Press [Enter] to maintain the current path: " + strFilePath);
        String strNewPath = null;
        System.out.print(Y + "File path: " + R);
        try {
            strNewPath = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (strNewPath.isEmpty()) {
            if (strFilePath == null)
                System.out.println("The file path setting is canceled.");
            else
                System.out.println("File path is maintained.");
        } else {
            strFilePath = strNewPath;
            System.out.println(Y + "New file path: " + R + strFilePath);
        }
        System.out.println("======");
    }

    public void checkFilePath() {
        if (strFilePath == null)
            System.out.println("No file path is set.");
        else
            System.out.println("Current file path: \"" + strFilePath + "\"");
    }

    public void pushFile() {
        if (m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState() != CMInfo.CM_CHAR) {
            System.err.println("Client is not logged in. You have to log in first to pushfile.");
            return;
        }

        JFileChooser fc = new JFileChooser();
        File filePath = new File(strFilePath);

        System.out.println(R + "#" + G + "Transfer files to Default Server" + R);
        if (!filePath.exists()) {
            filePath.mkdir();
            System.out.println(Y + "Directory " + R + strFilePath + Y + "is created" + R);
        }

        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        fc.setCurrentDirectory(filePath);
        if (fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
        {
            System.err.println("File Chooser is canceled");
            return;
        }

        File[] files = fc.getSelectedFiles();
        if (files.length < 1) {
            System.err.println("No file selected!");
            return;
        }
        for (File file : files)
            System.out.println(Y + "selected file = " + R + file.getName());

        for (File file : files) {
            if (m_clientStub.pushFile(file.getPath(), m_clientStub.getDefaultServerName()) == false) {
                System.err.println("Push file error!");
                return;
            }
        }
        System.out.println(B + "Files were transferred successfully!" + R);
        System.out.println("======");

        //        if (strFileName.isEmpty()) {
//            System.err.println("You must enter a file name.");
//            return;
//        }
//        if (m_clientStub.pushFile(strFilePath + strFileName, strReceiver) == false) ;
//        System.err.println("Push file error! file(" + strFileName + "), receiver(" + strReceiver + ")");
    }

    public static void main(String[] args) {
        CMClientApp client = new CMClientApp();
        CMClientStub cmStub = client.getClientStub();
        cmStub.setAppEventHandler(client.getClientEventHandler());
        client.StartCM();

        System.out.println(Y + "Client application is terminated.");
    }
}