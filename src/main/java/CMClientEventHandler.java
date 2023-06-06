import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CMClientEventHandler implements CMAppEventHandler {
    private final CMClientStub m_clientStub;
    private final JTextArea m_clientConsole;

    public CMClientEventHandler(CMClientStub stub, JTextArea clientConsole) {
        m_clientStub = stub;
        m_clientConsole = clientConsole;
    }

    @Override
    public void processEvent(CMEvent cme) {
        switch (cme.getType()) {
            case CMInfo.CM_DUMMY_EVENT -> {
                try {
                    processDummyEvent(cme);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case CMInfo.CM_SESSION_EVENT -> processSessionEvent(cme);
            case CMInfo.CM_FILE_EVENT -> m_clientStub.setTransferedFileHome(Paths.get(GUIClientApp.strClientFilePath));
            default -> {
            }
        }
    }

    private void processDummyEvent(CMEvent cme) throws IOException {
        System.out.println("Receive dummy event - info='" + ((CMDummyEvent) cme).getDummyInfo() + "' ID=" + cme.getID());
        String[] parsedInfo = ((CMDummyEvent) cme).getDummyInfo().split(":");
        String targetClient = parsedInfo[0];
        String fileName = parsedInfo[1];
        int serverLogicalClock = Integer.parseInt(parsedInfo[0]);
        int clientLogicalClock = Utils.findLogicalClock(fileName, GUIClientApp.clientFileList);
        switch (cme.getID()) {
            case EventID.FILESYNC_PUSH_ACCEPT -> {
                if (serverLogicalClock == -1)
                    m_clientConsole.append("FileSync: '" + fileName + "' does not exist on the server. Send file to server.\n");
                else
                    m_clientConsole.append("FileSync: '" + fileName + "' needs synchronizing. Send file to server.\n");
                m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
            }
            case EventID.FILESYNC_PUSH_REJECT -> {
                m_clientConsole.append("FileSync: '" + fileName + "' is in conflict.\n");
                m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
            }
            case EventID.FILESYNC_FILE_DELETE_ACCEPT -> {
                m_clientConsole.append("FileSync(Delete): '" + fileName + "' needs synchronizing. The file was deleted from the server.\n");
                m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
                Utils.deleteFileFromList(fileName, GUIClientApp.clientFileList);
            }
            case EventID.FILESYNC_FILE_DELETE_NOT_EXIST -> {
                m_clientConsole.append("FileSync(Delete): '" + fileName + "' is deleted but server does not have it.\n");
                m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
                Utils.deleteFileFromList(fileName, GUIClientApp.clientFileList);
            }
            case EventID.FILESYNC_FILE_DELETE_REJECT -> {
                m_clientConsole.append("FileSync(Delete): '" + fileName + "' is in conflict.\n");
                m_clientConsole.append("FileSync: LC at the time of synchronization - Client(" + clientLogicalClock + "), Server(" + serverLogicalClock + ")\n");
                Utils.deleteFileFromList(fileName, GUIClientApp.clientFileList);
            }
            case EventID.FILESHARE_NEWFILE_ACCEPT -> {
                m_clientConsole.append("FileShare: Push '" + fileName + "' to server");
                m_clientStub.pushFile(GUIClientApp.strClientFilePath + "\\" + fileName, "SERVER");
            }
            case EventID.FILESHARE_TARGETCLIENT_NOT_EXIST ->
                    m_clientConsole.append("FileShare: Client '" + targetClient + "' does not exist.\n");
            case EventID.FILESHARE_CONFLICT_OCCURED ->
                    m_clientConsole.append("FileShare: '" + fileName + "' is already shared - File is in conflict.\n");
            case EventID.FILESHARE_NEWFILE_RECEIVED ->
                    m_clientConsole.append("FileShare: '" + fileName + "' is shared by '" + targetClient + "'.\n");
            case EventID.FILESHARE_FILE_CASTED ->
                    m_clientConsole.append("FileShare: '" + fileName + "' is shared(synced) with client '" + targetClient + "'\n");
            case EventID.FILESHARE_TARGETFILE_NOT_SHARED ->
                    m_clientConsole.append("FileShare: '" + fileName + "' is not yet shared(synced)");
            case EventID.FILESHARESYNC_DELETE_REQUEST ->
                    processFileShareSyncDelete(targetClient, fileName);
            default -> {
            }
        }
    }

    private void processFileShareSyncDelete(String targetClient, String fileName) throws IOException {
        m_clientConsole.append("FileShare: " + fileName + "' is deleted in Client '" + targetClient + "'.");
        Files.delete(Path.of(GUIClientApp.strClientFilePath + "\\" + fileName));
    }

    private void processSessionEvent(CMEvent cme) {
        CMSessionEvent se = (CMSessionEvent) cme;
        switch (se.getID()) {
            case CMSessionEvent.LOGIN_ACK -> {
                if (se.isValidUser() == 0) // user authentication failed. (But since LOGIN_SCHEME == 0, there is no case.)
                {
                    m_clientConsole.append("This client fails authentication by the default server!\n");
                } else if (se.isValidUser() == -1) { // the same user already logged in
                    m_clientConsole.append("This client is already in the login-user list!\n");
                } else {
                    m_clientConsole.append("This client successfully logs in to the default server.\n");
                }
            }
            case CMSessionEvent.SESSION_ADD_USER ->
                    m_clientConsole.append("[" + se.getUserName() + "] logged in to this session!\n");
            case CMSessionEvent.SESSION_REMOVE_USER ->
                    m_clientConsole.append("[" + se.getUserName() + "] logged out of this session!\n");
            default -> {
            }
        }
    }
}

