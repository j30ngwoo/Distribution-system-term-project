import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import javax.swing.*;

public class CMClientEventHandler implements CMAppEventHandler {
    private CMClientStub m_clientStub;
    private JTextArea m_clientConsole;

    public CMClientEventHandler(CMClientStub stub, JTextArea clientConsole) {
        m_clientStub = stub;
        m_clientConsole = clientConsole;
    }

    @Override
    public void processEvent(CMEvent cme) {
        switch (cme.getType()) {
            case CMInfo.CM_SESSION_EVENT:
                processSessionEvent(cme);
                break;
            default:
                return;
        }
    }

    private void processSessionEvent(CMEvent cme) {
        CMSessionEvent se = (CMSessionEvent) cme;
        switch (se.getID()) {
            case CMSessionEvent.LOGIN_ACK:
                if (se.isValidUser() == 0) // user authentication failed. (But since LOGIN_SCHEME == 0, there is no case.)
                {
                    m_clientConsole.append("This client fails authentication by the default server!\n");
                } else if (se.isValidUser() == -1) { // the same user already logged in
                    m_clientConsole.append("This client is already in the login-user list!\n");
                } else {
                    m_clientConsole.append("This client successfully logs in to the default server.\n");
                }
                break;
            case CMSessionEvent.SESSION_ADD_USER:
                m_clientConsole.append("[" + se.getUserName() + "] logged in to this session!\n");
                break;
            case CMSessionEvent.SESSION_REMOVE_USER:
                m_clientConsole.append("[" + se.getUserName() + "] logged out of this session!\n");
                break;
            default:
                return;
        }
    }
}

