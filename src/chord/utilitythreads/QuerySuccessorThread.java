package chord.utilitythreads;

import chord.messages.Query;
import chord.node.NodeRecord;
import chord.node.Peer;
import chord.transport.TCPSender;

import java.io.IOException;
import java.net.Socket;

public class QuerySuccessorThread extends Thread {

    private Peer owner;
    private int queryInterval;
    private String ownerHost;
    private int ownerPort;
    TCPSender sender = new TCPSender();

    public QuerySuccessorThread(Peer owner, int queryInterval, String ownerHost, int ownerPort) {
        this.owner = owner;
        this.queryInterval = queryInterval;
        this.ownerHost = ownerHost;
        this.ownerPort = ownerPort;
    }

    private void queryOwnerSuccessor() {
        try {
            NodeRecord successor = owner.getFingerTable().get(1);
            Query query = new Query();
            sender.sendToSpecificSocket(successor.getNodeSocket(), query.getBytes());
            System.out.println("Sent a message to " + successor.toString() + " and the socket is closed: " + successor.getNodeSocket().isClosed());
//            System.out.println("sent a query");
        } catch (IOException e) {
            System.out.println("Could not contact successor, message was not sent.");
        } catch (NullPointerException npe) {
            System.out.println("Successor: " + ownerHost + ":" + ownerPort);
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
