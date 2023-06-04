import kr.ac.konkuk.ccslab.cm.entity.CMMember;
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
import java.util.ArrayList;

public class CMServerEventHandler implements CMAppEventHandler {
    public static ArrayList<ShareFileInfo> serverShareFileList = new ArrayList<>();
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
        String eventSender = cme.getSender();
        switch (cme.getID()) {
            case EventID.FILESYNC_FILECREATED:
                processSyncFileCreated(Integer.valueOf(parsedMessage[0]), parsedMessage[1], eventSender);
                break;
            case EventID.FILESYNC_FILEDELETED:
                processSyncFileDeleted(Integer.valueOf(parsedMessage[0]), parsedMessage[1], eventSender);
                break;
            case EventID.FILESYNC_FILEMODIFIED:
                processSyncFileModified(Integer.valueOf(parsedMessage[0]), parsedMessage[1], eventSender);
                break;
            case EventID.FILESHARE_REQUEST:
                processFileShareRequested(parsedMessage[0], parsedMessage[1], eventSender);
                break;
        }
    }

    private void processFileShareRequested(String targetClient, String fileName, String eventSender){
        System.out.println("# New file share requested: Sender=\'" + eventSender + "\' FileName=\'" + fileName + "\' TargetClient=" + targetClient);
        if (m_serverStub.getCMInfo().getInteractionInfo().getLoginUsers().isMember(targetClient))
        {
            int fileIndex = Utils.findFileFromList(fileName, serverShareFileList);
            if (fileIndex == -1) {
                registerNewShareFile(targetClient, fileName, eventSender);
            } else {
                registerNewClient(targetClient, fileName, eventSender, fileIndex);
            }
        }else{
            CMDummyEvent dummyEvent = new CMDummyEvent();
            dummyEvent.setDummyInfo(targetClient + ":" + null);
            dummyEvent.setID(EventID.FILESHARE_TARGETCLIENT_NOT_EXIST);
            m_serverStub.send(dummyEvent, eventSender);
        }
    }

    private void registerNewClient(String targetClient, String fileName, String eventSender, int fileIndex){
        ArrayList<String> clientList = serverShareFileList.get(fileIndex).sharedClients;
        System.out.println(Utils.findUserInStringArray(eventSender, clientList)); // test
        if (Utils.findUserInStringArray(eventSender, clientList) == -1){
            System.out.println("\'" + fileName + "\' already shared. Client '" + eventSender +"' is in conflict.");
            CMDummyEvent dummyEvent = new CMDummyEvent();
            dummyEvent.setDummyInfo(targetClient + ":" + fileName);
            dummyEvent.setID(EventID.FILESHARE_CONFLICT_OCCURED);
            m_serverStub.send(dummyEvent, eventSender);
        } else {
            if (Utils.findUserInStringArray(targetClient, clientList) == -1) {
                System.out.println("Client '" + targetClient + "\' does not share" + fileName + ". New Client added to Share list.");
                Utils.addClientToList(fileName, serverShareFileList, targetClient);
                CMDummyEvent dummyEvent = new CMDummyEvent();
                dummyEvent.setDummyInfo(targetClient + ":" + fileName);
                dummyEvent.setID(EventID.FILESHARE_NEWCLIENT_ACCEPT);
                m_serverStub.send(dummyEvent, eventSender);
                m_serverStub.requestFile(fileName, eventSender);
                castFile(fileName, eventSender);
            } else {
                System.out.println("Sender='" + eventSender + "' - Receiver='" + targetClient + "' already shared this file.");
                CMDummyEvent dummyEvent = new CMDummyEvent();
                dummyEvent.setDummyInfo(targetClient + ":" + fileName);
                dummyEvent.setID(EventID.FILESHARE_TARGETCLIENT_ALREADY_SHARE_THISFILE);
                m_serverStub.send(dummyEvent, eventSender);
            }
        }
    }

    private void registerNewShareFile(String targetClient, String fileName, String eventSender){
        System.out.println("\'" + fileName + "\' does not exist shared list. File added to list.");
        serverShareFileList.add(new ShareFileInfo(fileName, eventSender));
        Utils.addClientToList(fileName, serverShareFileList, targetClient);
        CMDummyEvent dummyEvent = new CMDummyEvent();
        dummyEvent.setDummyInfo(targetClient + ":" + fileName);
        dummyEvent.setID(EventID.FILESHARE_NEWFILE_ACCEPT);
        m_serverStub.send(dummyEvent, eventSender);
        m_serverStub.requestFile(fileName, eventSender);
        castFile(fileName, eventSender);
    }

    private void castFile(String fileName, String eventSender){
        ArrayList<String> clientList = null;
        CMDummyEvent dummyEvent = new CMDummyEvent();
        dummyEvent.setDummyInfo(eventSender + ":" + fileName);
        dummyEvent.setID(EventID.FILESHARE_FILEPUSH);
        for (int i = 0; i < serverShareFileList.size(); i++) {
            if (serverShareFileList.get(i).name.equals(fileName))
                clientList = serverShareFileList.get(i).sharedClients;
        }
        for (int i = 0; i < clientList.size(); i++){
            String targetClient = clientList.get(i);
            if (!targetClient.equals(eventSender)){
                System.out.println("test: filename=" + fileName + " targetClient=" + targetClient);
                m_serverStub.pushFile(".\\server-file-path\\" + clientList.get(0)
                        + "\\" + fileName, targetClient);
                m_serverStub.send(dummyEvent, targetClient);
            }
        }

    }

    private void processSyncFileCreated(Integer clientLogicalClock, String fileName, String eventSender){
        CMDummyEvent dummyEvent = new CMDummyEvent();
        Integer serverLogicalClock = Utils.findLogicalClock(fileName, CMServerApp.serverSyncFileList);
        dummyEvent.setDummyInfo(serverLogicalClock + ":" + fileName);
        if (serverLogicalClock == -1){
            System.out.println("# FileSync: \'" + fileName + "\' does not exist on the server. - add to file list");
            CMServerApp.serverSyncFileList.add(new SyncFileInfo(fileName, clientLogicalClock));
            m_serverStub.requestFile(fileName, eventSender);
            dummyEvent.setID(EventID.FILESYNC_PUSH_ACCEPT);
            m_serverStub.send(dummyEvent, eventSender);
        } else if (clientLogicalClock > serverLogicalClock) {
            System.out.println("# FileSync: Send a filePush request - LC : Client(" + clientLogicalClock + ") > Server(" + serverLogicalClock + ")");
            Utils.setLogicalClock(fileName, CMServerApp.serverSyncFileList, clientLogicalClock);
            m_serverStub.requestFile(fileName, eventSender);
            dummyEvent.setID(EventID.FILESYNC_PUSH_ACCEPT);
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
        Integer serverLogicalClock = Utils.findLogicalClock(fileName, CMServerApp.serverSyncFileList);
        dummyEvent.setDummyInfo(serverLogicalClock + ":" + fileName);
        if (serverLogicalClock == -1){
            System.out.println("# FileSync: File deleted in client does not exist in Server.");
            dummyEvent.setID(EventID.FILESYNC_FILE_DELETE_NOT_EXIST);
            m_serverStub.send(dummyEvent, eventSender);
        } else if (clientLogicalClock > serverLogicalClock) {
            System.out.println("# FileSync: File deleted in client - LC : Client(" + clientLogicalClock + ") > Server(" + serverLogicalClock + ")");
            Utils.deleteFileFromList(fileName, CMServerApp.serverSyncFileList);
            Files.delete(filePath);
            dummyEvent.setID(EventID.FILESYNC_FILE_DELETE_ACCEPT);
            m_serverStub.send(dummyEvent, eventSender);
        } else {
            System.out.println("# FileSync: File delete request rejected - LC : Client(" + clientLogicalClock + ") <= Server(" + serverLogicalClock + ")");
            dummyEvent.setID(EventID.FILESYNC_FILE_DELETE_REJECT);
            m_serverStub.send(dummyEvent, eventSender);
        }
    }

    private void processSyncFileModified(Integer clientLogicalClock, String fileName, String eventSender){
        CMDummyEvent dummyEvent = new CMDummyEvent();
        Integer serverLogicalClock = Utils.findLogicalClock(fileName, CMServerApp.serverSyncFileList);
        dummyEvent.setDummyInfo(serverLogicalClock + ":" + fileName);
        if (clientLogicalClock > serverLogicalClock) {
            System.out.println("# FileSync: Send a filePush request - LC : Client(" + clientLogicalClock + ") > Server(" + serverLogicalClock + ")");
            Utils.setLogicalClock(fileName, CMServerApp.serverSyncFileList, clientLogicalClock);
            m_serverStub.requestFile(fileName, eventSender);
            dummyEvent.setID(EventID.FILESYNC_PUSH_ACCEPT);
            m_serverStub.send(dummyEvent, eventSender);
        } else {
            System.out.println("# FileSync: FilePush request rejected - LC : Client(" + clientLogicalClock + ") <= Server(" + serverLogicalClock + ")");
            dummyEvent.setID(EventID.FILESYNC_PUSH_REJECT);
            m_serverStub.send(dummyEvent, eventSender);
        }
    }
}
