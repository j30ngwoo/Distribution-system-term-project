import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import javax.swing.*;
import java.io.IOException;

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
            case CMInfo.CM_DUMMY_EVENT:
                try {
                    processDummyEvent(cme);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case CMInfo.CM_SESSION_EVENT:
                processSessionEvent(cme);
                break;
            default:
                return;
        }
    }

    private void processDummyEvent(CMEvent cme) throws IOException {
        System.out.println("test: receive dummy event - info: " + ((CMDummyEvent)cme).getDummyInfo());
        //System.out.println("test: id - " + cme.getID());
        String[] parsedInfo = ((CMDummyEvent)cme).getDummyInfo().split(":");
        String fileName = parsedInfo[1];
        Integer serverLogicalClock = Integer.valueOf(parsedInfo[0]);
        Integer clientLogicalClock = Utils.findLogicalClock(fileName, GUIClientApp.clientFileList);
        //File file = new File(GUIClientApp.strClientFilePath + fileName);
        //System.out.println("test: file path: " + file.getCanonicalPath());
        switch (cme.getID()) {
            case EventID.FILESYNC_PUSH_REQUEST:
                if (serverLogicalClock == -1)
                    m_clientConsole.append("FileSync: \'" + fileName + "\' does not exist on the server. Send file to server.\n");
                else
                    m_clientConsole.append("FileSync: \'" + fileName + "\' needs synchronizing. Send file to server.\n");
                break;
            case EventID.FILESYNC_PUSH_REJECT:
                m_clientConsole.append("FileSync: \'" + fileName + "\' is in conflict.\n");
                break;
            case EventID.FILESYNC_FILE_DELETE_REQUEST:
                m_clientConsole.append("FileSync(Delete): \'" + fileName + "\' needs synchronizing. The file was deleted from the server.\n");
                Utils.deleteFileFromList(fileName, GUIClientApp.clientFileList);
                break;
            case EventID.FILESYNC_FILE_DELETE_NOT_EXIST:
                m_clientConsole.append("FileSync(Delete): \'" + fileName + "\' is deleted but server does not have it.\n");
                Utils.deleteFileFromList(fileName, GUIClientApp.clientFileList);
                break;
            case EventID.FILESYNC_FILE_DELETE_REJECT:
                m_clientConsole.append("FileSync(Delete): \'" + fileName + "\' is in conflict.\n");
                Utils.deleteFileFromList(fileName, GUIClientApp.clientFileList);
                break;
            default:
                return;
        }
        m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
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

