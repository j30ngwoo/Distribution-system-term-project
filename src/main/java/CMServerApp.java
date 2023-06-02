import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.List;

public class CMServerApp {
    private CMServerStub m_serverStub;
    private CMServerEventHandler m_eventHandler;
    private boolean m_bRun;

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
        startServer();
    }

    public void startServer() {
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
        System.out.println(Files.getFileAttributeView(Path.of(".\\1\\hello"), BasicFileAttributeView.class));
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