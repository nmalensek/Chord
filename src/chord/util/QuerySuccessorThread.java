package chord.util;

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

    private void queryOwnerSuccessor() throws IOException {
        NodeRecord successor = owner.getFingerTable().get(1);
        Socket successorSocket = new Socket(successor.getHost(), successor.getPort());
        try {
            Query query = new Query();
            sender.sendToSpecificSocket(successorSocket, query.getBytes());
        } finally {
            successorSocket.close();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
