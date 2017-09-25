package chord.utilitythreads;

import chord.messages.Query;
import chord.node.NodeRecord;
import chord.node.Peer;
import chord.transport.TCPSender;
import chord.util.FingerTableManagement;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class QuerySuccessorThread extends Thread {

    private Peer owner;
    private int queryInterval;
    private String ownerHost;
    private int ownerPort;
    private int ownerID;
    private TCPSender sender = new TCPSender();
    private FingerTableManagement fingerTableManagement = new FingerTableManagement();

    public QuerySuccessorThread(Peer owner, int queryInterval, String ownerHost, int ownerPort, int ownerID) {
        this.owner = owner;
        this.queryInterval = queryInterval;
        this.ownerHost = ownerHost;
        this.ownerPort = ownerPort;
        this.ownerID = ownerID;
    }

    private void queryOwnerSuccessor() {
        NodeRecord successor = null;
        try {
            successor = owner.getFingerTable().get(1);
            Query query = new Query();
            query.setSenderInfo(ownerHost + ":" + ownerPort + ":" + ownerID);
            sender.sendToSpecificSocket(successor.getNodeSocket(), query.getBytes());
//            System.out.println("Sent a message to " + successor.toString());
//            System.out.println("sent a query");
        } catch (IOException e) {
            System.out.println("Could not contact successor, message was not sent. Updating known nodes and finger table.");
            owner.getKnownNodes().remove(successor.getIdentifier());
            owner.getFingerTable().remove(successor.getIdentifier());
            fingerTableManagement.updateConcurrentFingerTable(ownerID, owner.getFingerTable(), owner.getKnownNodes());
        } catch (NullPointerException npe) {
//            System.out.println("Successor: " + ownerHost + ":" + ownerPort);
        }

    }

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(queryInterval);
                queryOwnerSuccessor();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
