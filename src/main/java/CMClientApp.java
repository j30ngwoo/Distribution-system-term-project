import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class CMClientApp {
    private static CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;
    private boolean m_bRun;

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
        System.out.println("========== start CM");
        System.out.println("my current address: " + strCurrentLocalAddress);
        System.out.println("saved server address: " + strSavedServerAddress);
        System.out.println("saved server port: " + nSavedServerPort);

        boolean bRet = m_clientStub.startCM();
        if (!bRet) {
            System.err.println("CM initialization error!");
            return;
        }
        startClient();
    }

    public void startClient() {
        System.out.println("client application starts.");
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
                    testTerminateCM();
                case 1: // connect to default server
                    testConnectionDS();
                    break;
                case 2: // disconnect from default server
                    testDisconnectionDS();
                    break;
                case 11: // synchronously login to default server
                    testSyncLoginDS();
                    break;
                case 12: // logout from default server
                    testLogoutDS();
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
        System.out.println("---------------------------------- Help");
        System.out.println("0: show all menus");
        System.out.println("---------------------------------- Stop");
        System.out.println("999: terminate CM");
        System.out.println("---------------------------------- Connection");
        System.out.println("1: connect to default server, 2: disconnect from default server");
        System.out.println("---------------------------------- Login");
        System.out.println("11: synchronously login to default server");
        System.out.println("12: logout from default server");
        System.out.println("----------------------------------");
    }

    public void testTerminateCM()
    {
        m_clientStub.terminateCM();
        m_bRun = false;
    }

    public void testConnectionDS()
    {
        System.out.println("====== connect to default server");
        m_clientStub.connectToServer();
        System.out.println("======");
    }

    public void testDisconnectionDS()
    {
        System.out.println("====== disconnect from default server");
        m_clientStub.disconnectFromServer();
        System.out.println("======");
    }

    public void testSyncLoginDS()
    {
        String strUserName = null;
        String strPassword = null;
        CMSessionEvent loginAckEvent = null;

        System.out.println("====== login to default server");
        System.out.print("user name: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            strUserName = br.readLine();
            System.out.print("password: ");
            strPassword = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        loginAckEvent = m_clientStub.syncLoginCM(strUserName, strPassword);
        if(loginAckEvent != null)
        {
            // print login result
            if(loginAckEvent.isValidUser() == 0)
            {
                System.err.println("This client fails authentication by the default server!");
            }
            else if(loginAckEvent.isValidUser() == -1)
            {
                System.err.println("This client is already in the login-user list!");
            }
            else
            {
                System.out.println("This client successfully logs in to the default server.");
            }
        }
        else
        {
            System.err.println("failed the login request!");
        }

        System.out.println("======");
    }

    public void testLogoutDS()
    {
        boolean bRequestResult = false;
        System.out.println("====== logout from default server");
        bRequestResult = m_clientStub.logoutCM();
        if(bRequestResult)
            System.out.println("successfully sent the logout request.");
        else
            System.err.println("failed the logout request!");
        System.out.println("======");
    }

    public static void main(String[] args) {
        CMClientApp client = new CMClientApp();
        CMClientStub cmStub = client.getClientStub();
        cmStub.setAppEventHandler(client.getClientEventHandler());
        client.StartCM();

        System.out.println("Client application is terminated.");
    }
}