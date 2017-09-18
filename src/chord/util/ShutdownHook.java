package chord.util;

import chord.messages.NodeLeaving;
import chord.node.NodeRecord;
import chord.node.Peer;
import chord.transport.TCPSender;

import java.io.IOException;
import java.net.Socket;

public class ShutdownHook extends Thread {

    private TCPSender sender;
    private Peer owner;
    private int ownerNodeID;
    private Socket discoveryNodeSocket;

    public ShutdownHook(TCPSender sender, Peer owner, int ownerNodeID,
                        Socket discoveryNodeSocket) {
        this.sender = sender;
        this.ownerNodeID = ownerNodeID;
        this.owner = owner;
        this.discoveryNodeSocket = discoveryNodeSocket;
    }

    @Override
    public void run() {
        NodeRecord successor = owner.getFingerTable().get(1);
        NodeRecord predecessor = owner.getPredecessor();

        NodeLeaving leaving = new NodeLeaving();
        leaving.setSixteenBitID(ownerNodeID);
        leaving.setSuccessorInfo(successor.getHost() + ":" + successor.getPort() + ":" + successor.getIdentifier());
        leaving.setPredecessorInfo(predecessor.getHost() + ":" + predecessor.getPort() + ":" + predecessor.getIdentifier());
        try {
            sender.sendToSpecificSocket(successor.getNodeSocket(), leaving.getBytes());
            sender.sendToSpecificSocket(predecessor.getNodeSocket(), leaving.getBytes());
            sender.sendToSpecificSocket(discoveryNodeSocket, leaving.getBytes());
            owner.sendFilesToSuccessor();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
