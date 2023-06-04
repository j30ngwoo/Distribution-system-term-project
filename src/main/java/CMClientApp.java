/*import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class Pair{
    String filename;
    Integer logicalClock;
    Pair(String filename, Integer logicalClock){
        this.filename = filename;
        this.logicalClock = logicalClock;
    }
}

public class CMClientApp extends JFrame{
    private static CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;
    private boolean m_bRun;
    private String strFilePath = null;
    public JFrame clientFrame = new JFrame();
    private JTextArea clientConsole = new JTextArea(40, 40);
    public ArrayList<Pair> clientFileInfo = new ArrayList<>();
    public static final String R = "\u001B[0m";
    public static final String G = "\u001B[32m";
    public static final String Y = "\u001B[33m";
    public static final String B = "\u001B[34m";
    public static final String M = "\u001B[35m";

    public CMClientApp() {
        m_clientStub = new CMClientStub();
        m_eventHandler = new CMClientEventHandler(m_clientStub, clientConsole);
        m_bRun = true;
    }

    public CMClientStub getClientStub() {
        return m_clientStub;
    }

    public CMClientEventHandler getClientEventHandler() {
        return m_eventHandler;
    }

    public void setGUI() {
        clientConsole.setBackground(Color.WHITE);
        clientConsole.setEditable(false);
        add(new JScrollPane(clientConsole));

        JButton loginButton = new JButton("Login/Logout");
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (m_clientStub.getMyself().getState() != CMInfo.CM_SESSION_JOIN)
                    loginDS();
                else
                    logoutDS();
            }
        });
        add(loginButton);

        JButton pushButton = new JButton("pushFile");
        pushButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pushFile();
            }
        });
        add(pushButton);

        JButton syncButton = new JButton("syncFile");
        add(syncButton);
        syncButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncFile();
            }
        });

        JButton shareButton = new JButton("shareFile");
        add(shareButton);


        setLayout(new FlowLayout());
        setTitle("Client");
        setSize(500, 740);
        setLocation(300, 100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void loginDS() {
        clientConsole.append("# Login to default server.\n");
        JFrame loginFrame = new JFrame();
        loginFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                clientConsole.append("Login failed\n");
            }
        });

        loginFrame.setLayout(new FlowLayout(FlowLayout.RIGHT));
        loginFrame.setTitle("login");
        loginFrame.setSize(280, 130);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);

        JLabel ID = new JLabel("ID: ");
        loginFrame.add(ID);

        JTextField IDInput = new JTextField(15);
        loginFrame.add(IDInput);

        JLabel PW = new JLabel("PASSWORD:");
        loginFrame.add(PW);

        JTextField PWInput = new JTextField(15);
        loginFrame.add(PWInput);

        JButton loginButton = new JButton("login");
        loginFrame.add(loginButton);

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String userName = IDInput.getText();
                String password = PWInput.getText();
                if (m_clientStub.loginCM(userName, password)) {
                    strFilePath = ".\\client-file-path-" + userName + "\\"; // initial set filepath
                    m_clientStub.setTransferedFileHome(Paths.get(strFilePath));
                    setTitle("Client " + userName);
                    loginFrame.dispose();
                } else {
                    clientConsole.append("failed the login request!\n");
                    loginFrame.dispose();
                }
                clientConsole.append("======\n");
            }
        });

    }

    public void logoutDS() {
        if (m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState() != CMInfo.CM_CHAR) {
            clientConsole.append("Client is not logged in.\n");
            return;
        }
        clientConsole.append("# Logout from default server.\n");
        if (m_clientStub.logoutCM())
            clientConsole.append("Successfully sent the logout request.\n");
        else
            clientConsole.append("Failed the logout request!\n");
        clientConsole.append("======\n");
    }

    public void startCM() {
        setGUI();
        List<String> localAddressList = CMCommManager.getLocalIPList();
        String strCurrentLocalAddress = localAddressList.get(0).toString();
        String strSavedServerAddress = null;
        int nSavedServerPort = -1;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(G + "# start CM");
        System.out.println(Y + "my current address: " + R + strCurrentLocalAddress);
        System.out.println(Y + "saved server address: " + R + m_clientStub.getServerAddress());
        System.out.println(Y + "saved server port: " + R + m_clientStub.getServerPort());
        clientConsole.append("# start CM\n");
        clientConsole.append("my current address: " + strCurrentLocalAddress + "\n");
        clientConsole.append("saved server address: " + m_clientStub.getServerAddress() + "\n");
        clientConsole.append("saved server port: " + m_clientStub.getServerPort() + "\n");
        clientConsole.append("======\n");

        boolean bRet = m_clientStub.startCM();
        if (!bRet) {
            System.err.println("CM initialization error!");
            clientConsole.append("CM initialization error!\n");
            return;
        }
        //startClient();
    }

    public void startClient() {
        System.out.println(G + "client application starts." + R);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String strInput = null;

        while (m_bRun) {

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
                    newClientFilePath();
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

    public void newClientFilePath() {
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
            clientConsole.append("Client is not logged in. You have to log in first to pushFile.\n");
            return;
        }

        JFileChooser pushFileChooser = new JFileChooser();
        File filePath = new File(strFilePath);

        clientConsole.append("# Transfer files to Default Server\n");
        if (!filePath.exists()) {
            filePath.mkdir();
            clientConsole.append("Directory \'" + strFilePath + "\' is created\n");
        }

        pushFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        pushFileChooser.setMultiSelectionEnabled(true);
        pushFileChooser.setCurrentDirectory(filePath);
        if (pushFileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
        {
            clientConsole.append("File Chooser is canceled\n");
            return;
        }

        File[] files = pushFileChooser.getSelectedFiles();
        if (files.length < 1) {
            clientConsole.append("No file selected!\n");
            return;
        }
        for (File file : files) {
            clientConsole.append("selected file = " + file.getName() + "\n");
        }

        for (File file : files) {
            if (m_clientStub.pushFile(file.getPath(), m_clientStub.getDefaultServerName()) == false) {
                clientConsole.append("Push file error!\n");
                return;
            }
        }
        clientConsole.append("Files were transferred successfully!\n");
        clientConsole.append("======\n");
    }

    public static void main(String[] args) {
        CMClientApp client = new CMClientApp();
        CMClientStub cmStub = client.getClientStub();
        cmStub.setAppEventHandler(client.getClientEventHandler());
        client.startCM();
    }
}*/