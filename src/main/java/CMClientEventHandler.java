import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Paths;

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
            case CMInfo.CM_FILE_EVENT:
                m_clientStub.setTransferedFileHome(Paths.get(GUIClientApp.strClientFilePath));
                break;
            default:
                return;
        }
    }

    private void processDummyEvent(CMEvent cme) throws IOException {
        System.out.println("test: receive dummy event - info: " + ((CMDummyEvent)cme).getDummyInfo());
        //System.out.println("test: id - " + cme.getID());
        String[] parsedInfo = ((CMDummyEvent)cme).getDummyInfo().split(":");
        String targetClient = parsedInfo[0];
        String fileName = parsedInfo[1];
        int serverLogicalClock = Integer.parseInt(parsedInfo[0]);
        int clientLogicalClock = Utils.findLogicalClock(fileName, GUIClientApp.clientFileList);
        //File file = new File(GUIClientApp.strClientFilePath + fileName);
        //System.out.println("test: file path: " + file.getCanonicalPath());
        switch (cme.getID()) {
            case EventID.FILESYNC_PUSH_ACCEPT:
                if (serverLogicalClock == -1)
                    m_clientConsole.append("FileSync: '" + fileName + "' does not exist on the server. Send file to server.\n");
                else
                    m_clientConsole.append("FileSync: '" + fileName + "' needs synchronizing. Send file to server.\n");
                m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
                break;
            case EventID.FILESYNC_PUSH_REJECT:
                m_clientConsole.append("FileSync: '" + fileName + "' is in conflict.\n");
                m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
                break;
            case EventID.FILESYNC_FILE_DELETE_ACCEPT:
                m_clientConsole.append("FileSync(Delete): '" + fileName + "' needs synchronizing. The file was deleted from the server.\n");
                m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
                Utils.deleteFileFromList(fileName, GUIClientApp.clientFileList);
                break;
            case EventID.FILESYNC_FILE_DELETE_NOT_EXIST:
                m_clientConsole.append("FileSync(Delete): '" + fileName + "' is deleted but server does not have it.\n");
                m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
                Utils.deleteFileFromList(fileName, GUIClientApp.clientFileList);
                break;
            case EventID.FILESYNC_FILE_DELETE_REJECT:
                m_clientConsole.append("FileSync(Delete): '" + fileName + "' is in conflict.\n");
                m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
                Utils.deleteFileFromList(fileName, GUIClientApp.clientFileList);
                break;
            case EventID.FILESHARE_NEWFILE_ACCEPT:
                m_clientConsole.append("FileShare: '" + fileName + "' is now shared with client '" + targetClient + "'\n");
                break;
            case EventID.FILESHARE_TARGETCLIENT_NOT_EXIST:
                m_clientConsole.append("FileShare: Client '" + targetClient + "' does not exist.\n");
                break;
            case EventID.FILESHARE_CONFLICT_OCCURED:
                m_clientConsole.append("FileShare: '" + fileName + "' is already shared - File is in conflict.\n");
                break;
            case EventID.FILESHARE_FILEPUSH:
                m_clientConsole.append("FileShare: '" + fileName + "' is shared by '" + targetClient + "'.\n");
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

