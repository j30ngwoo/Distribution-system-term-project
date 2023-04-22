import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

public class CMClientEventHandler implements CMAppEventHandler {
    private CMClientStub m_clientStub;

    public CMClientEventHandler(CMClientStub stub) {
        m_clientStub = stub;
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
                if (se.isValidUser() == 0) // user authentication failed. (But since LOGIN_SCHEME == 1, there is no case.)
                {
                    System.err.println("This client fails authentication by the default server!");
                } else if (se.isValidUser() == -1) // the same user already logged in
                {
                    System.err.println("This client is already in the login-user list!");
                } else {
                    System.out.println(CMClientApp.B + "This client successfully logs in to the default server." + CMClientApp.R);
                }
                break;
            case CMSessionEvent.SESSION_ADD_USER:
                System.out.println("[" + se.getUserName() + "]" + CMClientApp.B + " logged in to this session!" + CMClientApp.R);
                break;
            case CMSessionEvent.SESSION_REMOVE_USER:
                System.out.println("[" + se.getUserName() + "]" + CMClientApp.M + " logged out of this session!" + CMClientApp.R);
                break;
            default:
                return;
        }
    }
}

