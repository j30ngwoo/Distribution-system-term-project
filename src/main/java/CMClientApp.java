import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class CMClientApp {
    private static CMClientStub m_clientStub;
    private CMClientEventHandler m_eventHandler;
    public CMClientApp()
    {
        m_clientStub = new CMClientStub();
        m_eventHandler = new CMClientEventHandler(m_clientStub);
    }
    public CMClientStub getClientStub()
    {
        return m_clientStub;
    }
    public CMClientEventHandler getClientEventHandler()
    {
        return m_eventHandler;
    }
    public static void main(String[] args) {
        CMClientApp client = new CMClientApp();
        CMClientStub clientStub = client.getClientStub();
        CMClientEventHandler eventHandler = client.getClientEventHandler();
        boolean ret = false;

        // initialize CM
        clientStub.setAppEventHandler(eventHandler);
        ret = clientStub.startCM();
        if(ret)
            System.out.println("init success");
        else {
            System.err.println("init error!");
            return;
        }

        // login CM server
        String strUserName = null;
        String strPassword = null;
        CMSessionEvent loginAckEvent = null;
        Console console = System.console();

        System.out.print("user name: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            strUserName = br.readLine();
            if(console == null)
            {
                System.out.print("password: ");
                strPassword = br.readLine();
            }
            else
                strPassword = new String(console.readPassword("password: "));
        } catch (IOException e) {
            e.printStackTrace();
        }
        loginAckEvent = m_clientStub.syncLoginCM(strUserName, strPassword);
        if(loginAckEvent != null)
        {
            // print login result
            if(loginAckEvent.isValidUser() == 0)
                System.err.println("This client fails authentication by the default server!");
            else if(loginAckEvent.isValidUser() == -1)
                System.err.println("This client is already in the login-user list!");
            else
                System.out.println("This client successfully logs in to the default server.");
        }
        else
            System.err.println("failed the login request!");
    }
}