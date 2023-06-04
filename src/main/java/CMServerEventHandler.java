import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.beans.Introspector;

public class CMServerEventHandler implements CMAppEventHandler {
    private CMServerStub m_serverStub;

    public CMServerEventHandler(CMServerStub serverStub) {
        m_serverStub = serverStub;
    }

    @Override
    public void processEvent(CMEvent cme) {
        System.out.println("event occured : " + cme.getType() + " : " + cme.getID());
        switch (cme.getType()) {
            case CMInfo.CM_DUMMY_EVENT:
                processDummyEvent(cme);
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
        }
    }

    private void processDummyEvent(CMEvent cme) {
        System.out.println("test : dummy event: " + cme.getType() + " : " + cme.getID());
        String[] parsedMessage = ((CMDummyEvent)cme).getDummyInfo().split(":");
        System.out.println("test : dummy event info: " + parsedMessage[0] + "\n");
        Integer currentLogicalClock = Integer.valueOf(parsedMessage[0]);

        String eventSender = cme.getSender();
        System.out.println("test : dummy sender: " + eventSender + "\ncurrent LC: " + currentLogicalClock + "\n");
        switch (cme.getID()) {
            case EventID.FILESYNC_FILECREATED:

                processSyncFileCreated(currentLogicalClock, parsedMessage[1], eventSender);
            case EventID.FILESYNC_FILEDELETED:
                processSyncFileDeleted(currentLogicalClock, parsedMessage[1]);
            case EventID.FILESYNC_FILEMODIFIED:
                processSyncFileModified(currentLogicalClock, parsedMessage[1]);
        }
    }

    private void processSyncFileCreated(Integer clientLogicalClock, String fileName, String eventSender){
        CMDummyEvent dummyEvent = new CMDummyEvent();
        dummyEvent.setDummyInfo(fileName);
        Integer serverLogicalClock = Utils.findLogicalClock(fileName, CMServerApp.serverFileList);
        System.out.println(serverLogicalClock);
        System.out.println("test - cli LC: " + clientLogicalClock + "| FileName: " + fileName + "|eventSender: " + eventSender);
        if (clientLogicalClock > serverLogicalClock) {
            System.out.println("test1");
            System.out.println("#FileSync: Send a filePush request");
            dummyEvent.setID(EventID.FILESYNC_PUSH_REQUEST);
            m_serverStub.send(dummyEvent, eventSender);
        } else {
            System.out.println("test2");
            System.out.println("#FileSync: FilePush rejected");
            dummyEvent.setID(EventID.FILESYNC_PUSH_REJECT);
            m_serverStub.send(dummyEvent, eventSender);
        }
        System.out.println("test end");
    }

    private void processSyncFileDeleted(Integer clientLogicalClock, String fileName){

    }

    private void processSyncFileModified(Integer clientLogicalClock, String fileName){

    }

}
