import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.handler.CMAppEventHandler;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.stub.CMServerStub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

public class CMServerEventHandler implements CMAppEventHandler {
    public static ArrayList<ShareFileInfo> serverShareFileList = new ArrayList<>();
    private final CMServerStub m_serverStub;

    public CMServerEventHandler(CMServerStub serverStub) {
        m_serverStub = serverStub;
    }

    @Override
    public void processEvent(CMEvent cme) {
        switch (cme.getType()) {
            case CMInfo.CM_DUMMY_EVENT -> {
                try {
                    processDummyEvent(cme);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            case CMInfo.CM_SESSION_EVENT -> processSessionEvent(cme);
            case CMInfo.CM_FILE_EVENT -> processFileEvent(cme);
            default -> {
            }
        }
    }

    private void processSessionEvent(CMEvent cme) {
        CMSessionEvent se = (CMSessionEvent) cme;
        switch (se.getID()) {
            case CMSessionEvent.LOGIN ->
                    System.out.println("[" + se.getUserName() + "]" + GUIClientApp.B + " requests login." + GUIClientApp.R);
            case CMSessionEvent.LOGOUT ->
                    System.out.println("[" + se.getUserName() + "]" + GUIClientApp.B + " logs out." + GUIClientApp.R);
            default -> {
            }
        }
    }

    private void processFileEvent(CMEvent cme) {
        CMFileEvent fe = (CMFileEvent) cme;
        if (fe.getID() == CMFileEvent.END_FILE_TRANSFER) {
            System.out.println("[" + fe.getFileSender() + "]" + GUIClientApp.B + " completes to send file: " + GUIClientApp.R + fe.getFileName());
        }
    }

    private void processDummyEvent(CMEvent cme) throws IOException, InterruptedException {
        String[] parsedMessage = ((CMDummyEvent) cme).getDummyInfo().split(":");
        String eventSender = cme.getSender();
        switch (cme.getID()) {
            case EventID.FILESYNC_FILECREATED ->
                    processSyncFileCreated(Integer.valueOf(parsedMessage[0]), parsedMessage[1], eventSender);
            case EventID.FILESYNC_FILEDELETED ->
                    processSyncFileDeleted(Integer.valueOf(parsedMessage[0]), parsedMessage[1], eventSender);
            case EventID.FILESYNC_FILEMODIFIED ->
                    processSyncFileModified(Integer.valueOf(parsedMessage[0]), parsedMessage[1], eventSender);
            case EventID.FILESHARE_REQUEST ->
                    processFileShareRequested(parsedMessage[0], parsedMessage[1], eventSender);
            case EventID.FILESHARESYNC_DELETE_REQUEST ->
                    processFileShareSyncDeleted(parsedMessage[1], eventSender);
            case EventID.FILESHARESYNC_MODIFY_REQUEST ->
                    processFileShareSyncModified(parsedMessage[1], eventSender);
        }
    }

    private void processFileShareSyncDeleted(String fileName, String eventSender)
    {
        if (Utils.findFileFromList(fileName, serverShareFileList) == -1) {
            CMDummyEvent dummyEvent = new CMDummyEvent();
            dummyEvent.setDummyInfo("null" + ":" + fileName);
            dummyEvent.setID(EventID.FILESHARE_TARGETFILE_NOT_SHARED);
            m_serverStub.send(dummyEvent, eventSender);
        } else {
            castFileDelete(fileName, eventSender);
        }
    }

    private void processFileShareSyncModified(String fileName, String eventSender) throws IOException {
        int fileIndex = Utils.findFileFromList(fileName, serverShareFileList);
        System.out.println("test: fileindex = " + fileIndex + "  sender = " + eventSender);
        if (fileIndex == -1) {
            CMDummyEvent dummyEvent = new CMDummyEvent();
            dummyEvent.setDummyInfo("null" + ":" + fileName);
            dummyEvent.setID(EventID.FILESHARE_TARGETFILE_NOT_SHARED);
            m_serverStub.send(dummyEvent, eventSender);
        } else {
            File file = new File(".\\server-file-path\\" + eventSender + "\\" + fileName);
            if (file.exists())
                Files.delete(Path.of(".\\server-file-path\\" + eventSender + "\\" + fileName));
            m_serverStub.requestFile(fileName, eventSender);
            Runnable task = () -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                while (!file.exists()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                castFile(fileName, eventSender);
            };
            Thread subTread1 = new Thread(task);
            subTread1.start();
        }
    }

    private void castFileDelete(String fileName, String eventSender) {
        ArrayList<String> clientList = null;
        CMDummyEvent dummyEvent = new CMDummyEvent();
        dummyEvent.setDummyInfo(eventSender + ":" + fileName);
        dummyEvent.setID(EventID.FILESHARESYNC_DELETE_REQUEST);
        for (ShareFileInfo shareFileInfo : serverShareFileList) {
            if (shareFileInfo.name.equals(fileName))
                clientList = shareFileInfo.sharedClients;
        }
        for (String targetClient : Objects.requireNonNull(clientList)) {
            System.out.println("castFileDelete: filename=" + fileName + " targetClient=" + targetClient);
            m_serverStub.send(dummyEvent, targetClient);
        }
    }


    private void processFileShareRequested(String targetClient, String fileName, String eventSender) throws IOException {
        System.out.println("# New file share requested: Sender='" + eventSender + "' FileName='" + fileName + "' TargetClient='" + targetClient + "'");
        if (m_serverStub.getCMInfo().getInteractionInfo().getLoginUsers().isMember(targetClient)) {
            int fileIndex = Utils.findFileFromList(fileName, serverShareFileList);
            if (fileIndex == -1) {
                registerNewShareFile(targetClient, fileName, eventSender);
            } else {
                registerNewClient(targetClient, fileName, eventSender, fileIndex);
            }
        } else {
            CMDummyEvent dummyEvent = new CMDummyEvent();
            dummyEvent.setDummyInfo(targetClient + ":" + null);
            dummyEvent.setID(EventID.FILESHARE_TARGETCLIENT_NOT_EXIST);
            m_serverStub.send(dummyEvent, eventSender);
        }
    }

    private void registerNewClient(String targetClient, String fileName, String eventSender, int fileIndex) throws IOException {
        ArrayList<String> clientList = serverShareFileList.get(fileIndex).sharedClients;
        System.out.println(Utils.findUserInStringArray(eventSender, clientList)); // test
        if (Utils.findUserInStringArray(eventSender, clientList) == -1) {
            System.out.println("'" + fileName + "' already shared. Client '" + eventSender + "' is in conflict.");
            CMDummyEvent dummyEvent = new CMDummyEvent();
            dummyEvent.setDummyInfo(targetClient + ":" + fileName);
            dummyEvent.setID(EventID.FILESHARE_CONFLICT_OCCURED);
            m_serverStub.send(dummyEvent, eventSender);
        } else {
            if (Utils.findUserInStringArray(targetClient, clientList) == -1) {
                System.out.println("Client '" + targetClient + "' does not share '" + fileName + "'. New Client added to Share list.");
                Utils.addClientToList(fileName, serverShareFileList, targetClient);
                CMDummyEvent dummyEvent = new CMDummyEvent();
                dummyEvent.setDummyInfo(targetClient + ":" + fileName);
                dummyEvent.setID(EventID.FILESHARE_NEWCLIENT_ACCEPT);
                m_serverStub.send(dummyEvent, eventSender);
                Files.delete(Path.of(".\\server-file-path\\" + eventSender + "\\" + fileName));
                m_serverStub.requestFile(fileName, eventSender);
                Runnable task = () -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    File file = new File(".\\server-file-path\\" + eventSender + "\\" + fileName);
                    while (!file.exists()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    castFile(fileName, eventSender);
                };
                Thread subTread1 = new Thread(task);
                subTread1.start();
            } else {
                System.out.println("Sender='" + eventSender + "' - Receiver='" + targetClient + "' already shared this file.");
                CMDummyEvent dummyEvent = new CMDummyEvent();
                dummyEvent.setDummyInfo(targetClient + ":" + fileName);
                dummyEvent.setID(EventID.FILESHARE_TARGETCLIENT_ALREADY_SHARE_THISFILE);
                m_serverStub.send(dummyEvent, eventSender);
            }
        }
    }

    private void registerNewShareFile(String targetClient, String fileName, String eventSender) {
        System.out.println("'" + fileName + "' does not exist shared list. File added to list.");
        serverShareFileList.add(new ShareFileInfo(fileName, eventSender));
        Utils.addClientToList(fileName, serverShareFileList, targetClient);
        CMDummyEvent dummyEvent = new CMDummyEvent();
        dummyEvent.setDummyInfo(targetClient + ":" + fileName);
        dummyEvent.setID(EventID.FILESHARE_NEWFILE_ACCEPT);
        m_serverStub.send(dummyEvent, eventSender);

        Runnable task = () -> {
            File file = new File(".\\server-file-path\\" + eventSender + "\\" + fileName);
            while (!file.exists()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            castFile(fileName, eventSender);
        };
        Thread subTread1 = new Thread(task);
        subTread1.start();
    }

    private void castFile(String fileName, String eventSender) {
        ArrayList<String> clientList = null;
        CMDummyEvent dummyEvent = new CMDummyEvent();
        dummyEvent.setDummyInfo(eventSender + ":" + fileName);
        dummyEvent.setID(EventID.FILESHARE_NEWFILE_RECEIVED);
        for (ShareFileInfo shareFileInfo : serverShareFileList) {
            if (shareFileInfo.name.equals(fileName))
                clientList = shareFileInfo.sharedClients;
        }
        for (String targetClient : Objects.requireNonNull(clientList)) {
            if (!targetClient.equals(eventSender)) {
                System.out.println("test: filename=" + fileName + " targetClient=" + targetClient);
                m_serverStub.pushFile(".\\server-file-path\\" + eventSender
                        + "\\" + fileName, targetClient);
                m_serverStub.send(dummyEvent, targetClient);
                CMDummyEvent dummyEventToEventSender = new CMDummyEvent();
                dummyEventToEventSender.setDummyInfo(targetClient + ":" + fileName);
                dummyEventToEventSender.setID(EventID.FILESHARE_FILE_CASTED);
                m_serverStub.send(dummyEventToEventSender, eventSender);
            }
        }
        System.out.println("test: cast end");
    }

    private void processSyncFileCreated(Integer clientLogicalClock, String fileName, String eventSender) {
        CMDummyEvent dummyEvent = new CMDummyEvent();
        Integer serverLogicalClock = Utils.findLogicalClock(fileName, CMServerApp.serverSyncFileList);
        dummyEvent.setDummyInfo(serverLogicalClock + ":" + fileName);
        if (serverLogicalClock == -1) {
            System.out.println("# FileSync: '" + fileName + "' does not exist on the server. - add to file list");
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
        if (serverLogicalClock == -1) {
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

    private void processSyncFileModified(Integer clientLogicalClock, String fileName, String eventSender) throws IOException {
        CMDummyEvent dummyEvent = new CMDummyEvent();
        Integer serverLogicalClock = Utils.findLogicalClock(fileName, CMServerApp.serverSyncFileList);
        dummyEvent.setDummyInfo(serverLogicalClock + ":" + fileName);
        if (clientLogicalClock > serverLogicalClock) {
            System.out.println("# FileSync: Send a filePush request - LC : Client(" + clientLogicalClock + ") > Server(" + serverLogicalClock + ")");
            Utils.setLogicalClock(fileName, CMServerApp.serverSyncFileList, clientLogicalClock);
            Files.delete(Path.of(".\\server-file-path\\" + eventSender + "\\" + fileName));
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
