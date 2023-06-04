import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class GUIClientApp extends JFrame{
    private static CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;
    private boolean m_bRun;
    public static String strClientFilePath = null;
    public JFrame clientFrame = new JFrame();
    static JTextArea clientConsole = new JTextArea(40, 40);
    public static ArrayList<SyncFileInfo> clientFileList = new ArrayList<>();
    public static final String R = "\u001B[0m";
    public static final String G = "\u001B[32m";
    public static final String Y = "\u001B[33m";
    public static final String B = "\u001B[34m";
    public static final String M = "\u001B[35m";

    public GUIClientApp() {
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

    private void setGUI() {
        clientConsole.setBackground(Color.WHITE);
        clientConsole.setEditable(false);
        add(new JScrollPane(clientConsole));

        JButton loginButton = new JButton("Login/Logout");
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clientConsole.append(Integer.toString(m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState()));
                if (m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState() != CMInfo.CM_SESSION_JOIN)
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
                try {
                    SyncFile.syncFile();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        JButton shareButton = new JButton("shareFile");
        shareButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ShareFile.setShareFileFrame();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        add(shareButton);

        setLayout(new FlowLayout());
        setTitle("Client");
        setSize(500, 740);
        setLocation(300, 100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    class ShareFile {
        static String targetClientInputText;
        static File shareFile;
        public static void setShareFileFrame() throws IOException, InterruptedException {
            if (m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState() != CMInfo.CM_CHAR) {
                clientConsole.append("Client is not logged in. You have to log in first to shareFile.\n");
                return;
            }
            m_clientStub.setTransferedFileHome(Paths.get(strClientFilePath));
            clientConsole.append("# Share files with other clients\n");
            JFrame shareFrame = new JFrame();
            shareFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    clientConsole.append("File share failed\n");
                    shareFrame.dispose();
                }
            });

            shareFrame.setLayout(new FlowLayout(FlowLayout.RIGHT));
            shareFrame.setTitle("File share");
            shareFrame.setSize(310, 100);
            shareFrame.setLocationRelativeTo(null);
            shareFrame.setVisible(true);

            JLabel targetClient = new JLabel("Client to share file: ");
            shareFrame.add(targetClient);

            JTextField targetClientInput = new JTextField(15);
            shareFrame.add(targetClientInput);

            JButton shareButton = new JButton("Share");
            shareFrame.add(shareButton);

            shareButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    targetClientInputText = targetClientInput.getText();
                    shareFile(targetClientInputText);
                    shareFrame.dispose();
                }
            });
        }

        private static void shareFile(String targetClient){
            JFileChooser pushFileChooser = new JFileChooser();
            File filePath = new File(strClientFilePath);

            pushFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            pushFileChooser.setMultiSelectionEnabled(false);
            pushFileChooser.setCurrentDirectory(filePath);
            if (pushFileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
            {
                clientConsole.append("File Chooser is canceled\n");
                return;
            }

            shareFile = pushFileChooser.getSelectedFile();
            if (shareFile == null) {
                clientConsole.append("No file selected!\n");
                return;
            }
            String shareFileName = shareFile.getName();
            clientConsole.append("selected file = " + shareFileName + "\n");

            CMDummyEvent dummyEvent = new CMDummyEvent();
            dummyEvent.setDummyInfo(targetClient + ":" + shareFileName);
            dummyEvent.setID(EventID.FILESHARE_REQUEST);
            m_clientStub.send(dummyEvent, "SERVER");
        }
    }

    class SyncFile {
        public static void syncFile() throws IOException, InterruptedException {
            if (m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState() != CMInfo.CM_CHAR) {
                clientConsole.append("Client is not logged in. You have to log in first to syncFile.\n");
                return;
            }
            m_clientStub.setTransferedFileHome(Paths.get(strClientFilePath));
            clientConsole.append("# Synchronize files with the server\n");
            trackFile();
        }

        public static void trackFile() throws IOException, InterruptedException {
            WatchService service = FileSystems.getDefault().newWatchService();
            Path dir = Paths.get(strClientFilePath).toAbsolutePath();
            System.out.println(dir.toString());
            dir.register(service,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            while (true) {
                WatchKey key = service.take();
                List<WatchEvent<?>> list = key.pollEvents();
                for(WatchEvent<?> event : list){
                    WatchEvent.Kind<?> kind = event.kind();
                    String eventFileName = ((Path)event.context()).getFileName().toString();
                    if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        fileCreated(eventFileName);
                        return ;
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        fileDeleted(eventFileName);
                        return ;
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                        fileModified(eventFileName);
                        return;
                    }
                }
                if (!key.reset()) break;
            }
            service.close();
        }
        public static void fileCreated(String fileName) {
            clientConsole.append(" - New file created: " + fileName + "\n");
            clientFileList.add(new SyncFileInfo(fileName, 1));
            CMDummyEvent dummyEvent = new CMDummyEvent();
            dummyEvent.setDummyInfo("1:" + fileName);
            dummyEvent.setID(EventID.FILESYNC_FILECREATED);
            m_clientStub.send(dummyEvent, "SERVER");
        }

        public static void fileDeleted(String fileName) {
            clientConsole.append(" - File Deleted: " + fileName + "\n");
            Integer LC = Utils.increaseLogicalClock(fileName, clientFileList);
            CMDummyEvent dummyEvent = new CMDummyEvent();
            dummyEvent.setDummyInfo(LC + ":" + fileName);
            dummyEvent.setID(EventID.FILESYNC_FILEDELETED);
            m_clientStub.send(dummyEvent, "SERVER");
            //Utils.deleteFileFromList(fileName, clientFileList);
        }

        public static void fileModified(String fileName) {
            clientConsole.append(" - File modified: " + fileName + "\n");
            Integer LC = Utils.increaseLogicalClock(fileName, clientFileList);
            CMDummyEvent dummyEvent = new CMDummyEvent();
            dummyEvent.setDummyInfo(LC + ":" + fileName);
            dummyEvent.setID(EventID.FILESYNC_FILEMODIFIED);
            m_clientStub.send(dummyEvent, "SERVER");
        }
    }

    public void loginDS() {
        clientConsole.append("# Login to default server.\n");
        JFrame loginFrame = new JFrame();
        loginFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                clientConsole.append("Login failed\n");
                loginFrame.dispose();
            }
        });

        loginFrame.setLayout(new FlowLayout(FlowLayout.RIGHT));
        loginFrame.setTitle("Login");
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
                    strClientFilePath = ".\\client-file-path-" + userName + "\\"; // initial set filepath
                    File filePath = new File(strClientFilePath);
                    if (!filePath.exists()) {
                        filePath.mkdir();
                        clientConsole.append("Directory \'" + strClientFilePath + "\' is created\n");
                    }
                    m_clientStub.setTransferedFileHome(Paths.get(strClientFilePath));
                    setTitle("Client " + userName);
                    loginFrame.dispose();
                    setFileList();
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
        }
        //startClient();
    }

    private void setFileList() {
        String[] fileNames = new File(strClientFilePath).list();

        for (int i = 0; i < fileNames.length; i++) {
            clientConsole.append("File detected: \'" + fileNames[i] + "\' - adding to file list\n");
            clientFileList.add(new SyncFileInfo(fileNames[i], 1));
        }
    }

    public void newClientFilePath() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("#" + G + "set file path.");
        if (strClientFilePath != null)
            System.out.println("Press [Enter] to maintain the current path: " + strClientFilePath);
        String strNewPath = null;
        System.out.print(Y + "File path: " + R);
        try {
            strNewPath = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (strNewPath.isEmpty()) {
            if (strClientFilePath == null)
                System.out.println("The file path setting is canceled.");
            else
                System.out.println("File path is maintained.");
        } else {
            strClientFilePath = strNewPath;
            System.out.println(Y + "New file path: " + R + strClientFilePath);
        }
        System.out.println("======");
    }

    public void checkFilePath() {
        if (strClientFilePath == null)
            System.out.println("No file path is set.");
        else
            System.out.println("Current file path: \"" + strClientFilePath + "\"");
    }

    public void pushFile() {
        if (m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState() != CMInfo.CM_CHAR) {
            clientConsole.append("Client is not logged in. You have to log in first to pushFile.\n");
            return;
        }

        JFileChooser pushFileChooser = new JFileChooser();
        File filePath = new File(strClientFilePath);
        if (!filePath.exists()) {
            filePath.mkdir();
            clientConsole.append("Directory \'" + strClientFilePath + "\' is created\n");
        }

        clientConsole.append("# Transfer files to Default Server\n");

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
            if (m_clientStub.pushFile(file.getPath(), "SERVER") == false) {
                clientConsole.append("Push file error!\n");
                return;
            }
        }
        clientConsole.append("Files were transferred successfully!\n");
        clientConsole.append("======\n");
    }

    public static void main(String[] args) {
        GUIClientApp client = new GUIClientApp();
        CMClientStub cmStub = client.getClientStub();
        cmStub.setAppEventHandler(client.getClientEventHandler());
        client.startCM();
    }
}