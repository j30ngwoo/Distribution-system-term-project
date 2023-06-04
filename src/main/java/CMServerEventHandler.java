import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CMServerEventHandler implements CMAppEventHandler {
    private CMServerStub m_serverStub;

    public CMServerEventHandler(CMServerStub serverStub) {
        m_serverStub = serverStub;
    }

    @Override
    public void processEvent(CMEvent cme) {
        //System.out.println("event occured : " + cme.getType() + " : " + cme.getID());
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
                processFileEvent(cme);
                break;
            default:
                return;
        }
    }

    private void processSessionEvent(CMEvent cme) {
        CMSessionEvent se = (CMSessionEvent) cme;

        switch (se.getID()) {
            case CMSessionEvent.LOGIN:
                System.out.println("[" + se.getUserName() + "]" + GUIClientApp.B + " requests login." + GUIClientApp.R);
                break;
            case CMSessionEvent.LOGOUT:
                System.out.println("[" + se.getUserName() + "]" + GUIClientApp.B + " logs out." + GUIClientApp.R);
                break;
            default:
                return;
        }
    }

    private void processFileEvent(CMEvent cme) {
        CMFileEvent fe = (CMFileEvent) cme;
        switch (fe.getID()) {
            case CMFileEvent.END_FILE_TRANSFER:
                System.out.println("[" + fe.getFileSender() + "]" + GUIClientApp.B + " completes to send file: " + GUIClientApp.R + fe.getFileName());
                break;
        }
    }

    private void processDummyEvent(CMEvent cme) throws IOException {
        //System.out.println("test : dummy event: " + cme.getType() + " : " + cme.getID());
        String[] parsedMessage = ((CMDummyEvent)cme).getDummyInfo().split(":");
        Integer currentLogicalClock = Integer.valueOf(parsedMessage[0]);

        String eventSender = cme.getSender();
        switch (cme.getID()) {
            case EventID.FILESYNC_FILECREATED:
                processSyncFileCreated(currentLogicalClock, parsedMessage[1], eventSender);
                break;
            case EventID.FILESYNC_FILEDELETED:
                processSyncFileDeleted(currentLogicalClock, parsedMessage[1], eventSender);
                break;
            case EventID.FILESYNC_FILEMODIFIED:
                processSyncFileModified(currentLogicalClock, parsedMessage[1], eventSender);
                break;
        }
    }

    private void processSyncFileCreated(Integer clientLogicalClock, String fileName, String eventSender){
        CMDummyEvent dummyEvent = new CMDummyEvent();
        Integer serverLogicalClock = Utils.findLogicalClock(fileName, CMServerApp.serverFileList);
        dummyEvent.setDummyInfo(serverLogicalClock + ":" + fileName);
        if (serverLogicalClock == -1){
            System.out.println("# FileSync: \'" + fileName + "\' does not exist on the server. - add to file list");
            CMServerApp.serverFileList.add(new FileInfo(fileName, null, clientLogicalClock));
            m_serverStub.requestFile(fileName, eventSender);
            dummyEvent.setID(EventID.FILESYNC_PUSH_REQUEST);
            m_serverStub.send(dummyEvent, eventSender);
        } else if (clientLogicalClock > serverLogicalClock) {
            System.out.println("# FileSync: Send a filePush request - LC : Client(" + clientLogicalClock + ") > Server(" + serverLogicalClock + ")");
            Utils.setLogicalClock(fileName, CMServerApp.serverFileList, clientLogicalClock);
            m_serverStub.requestFile(fileName, eventSender);
            dummyEvent.setID(EventID.FILESYNC_PUSH_REQUEST);
            m_serverStub.send(dummyEvent, eventSender);
        } else {
            System.out.println("# FileSync: FilePush request rejected - LC : Client(" + clientLogicalClock + ") <= Server(" + serverLogicalClock + ")");
            dummyEvent.setID(EventID.FILESYNC_PUSH_REJECT);
            m_serverStub.send(dummyEvent, eventSender);
        }
    }

    private void processSyncFileDeleted(Integer clientLogicalClock, String fileName, String eventSender) throws IOException {
        CMDummyEvent dummyEvent = new CMDummyEvent();
        Path filePath = Path.of(".\\server-file-path\\" + eventSender + "\\" + fileName);
        Integer serverLogicalClock = Utils.findLogicalClock(fileName, CMServerApp.serverFileList);
        dummyEvent.setDummyInfo(serverLogicalClock + ":" + fileName);
        if (serverLogicalClock == -1){
            System.out.println("# FileSync: File deleted in client does not exist in Server.");
            dummyEvent.setID(EventID.FILESYNC_FILE_DELETE_NOT_EXIST);
            m_serverStub.send(dummyEvent, eventSender);
        } else if (clientLogicalClock > serverLogicalClock) {
            System.out.println("# FileSync: File deleted in client - LC : Client(" + clientLogicalClock + ") > Server(" + serverLogicalClock + ")");
            Utils.deleteFileFromList(fileName, CMServerApp.serverFileList);
            Files.delete(filePath);
            dummyEvent.setID(EventID.FILESYNC_FILE_DELETE_REQUEST);
            m_serverStub.send(dummyEvent, eventSender);
        } else {
            System.out.println("# FileSync: File delete request rejected - LC : Client(" + clientLogicalClock + ") <= Server(" + serverLogicalClock + ")");
            dummyEvent.setID(EventID.FILESYNC_FILE_DELETE_REJECT);
            m_serverStub.send(dummyEvent, eventSender);
        }
    }

    private void processSyncFileModified(Integer clientLogicalClock, String fileName, String eventSender){
        CMDummyEvent dummyEvent = new CMDummyEvent();
        Integer serverLogicalClock = Utils.findLogicalClock(fileName, CMServerApp.serverFileList);
        dummyEvent.setDummyInfo(serverLogicalClock + ":" + fileName);
        if (clientLogicalClock > serverLogicalClock) {
            System.out.println("# FileSync: Send a filePush request - LC : Client(" + clientLogicalClock + ") > Server(" + serverLogicalClock + ")");
            Utils.setLogicalClock(fileName, CMServerApp.serverFileList, clientLogicalClock);
            m_serverStub.requestFile(fileName, eventSender);
            dummyEvent.setID(EventID.FILESYNC_PUSH_REQUEST);
            m_serverStub.send(dummyEvent, eventSender);
        } else {
            System.out.println("# FileSync: FilePush request rejected - LC : Client(" + clientLogicalClock + ") <= Server(" + serverLogicalClock + ")");
            dummyEvent.setID(EventID.FILESYNC_PUSH_REJECT);
            m_serverStub.send(dummyEvent, eventSender);
        }
    }
}
