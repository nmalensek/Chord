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
    TCPSender sender = new TCPSender();

    public QuerySuccessorThread(Peer owner, int queryInterval) {
        this.owner = owner;
        this.queryInterval = queryInterval;
    }

    private void queryOwnerSuccessor() {
        try {
            NodeRecord successor = owner.getFingerTable().get(1);
            Query query = new Query();
            sender.sendToSpecificSocket(successor.getNodeSocket(), query.getBytes());
        } catch (IOException e) {
            System.out.println("Could not contact successor, message was not sent.");
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
