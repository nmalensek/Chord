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
    private String discoveryNodeHost;
    private int discoveryNodePort;

    public ShutdownHook(TCPSender sender, Peer owner, int ownerNodeID,
                        String discoveryNodeHost, int discoveryNodePort) {
        this.sender = sender;
        this.ownerNodeID = ownerNodeID;
        this.owner = owner;
        this.discoveryNodeHost = discoveryNodeHost;
        this.discoveryNodePort = discoveryNodePort;
    }

    //TODO transfer files to successor!
    //tell predecessor about your successor

    @Override
    public void run() {
        NodeRecord successor = owner.getFingerTable().get(1);
        NodeRecord predecessor = owner.getPredecessor();

        NodeLeaving leaving = new NodeLeaving();
        leaving.setSixteenBitID(ownerNodeID);
        leaving.setPredecessorInfo(successor.getHost() + ":" + successor.getPort() + ":" + successor.getIdentifier());
        leaving.setPredecessorInfo(predecessor.getHost() + ":" + predecessor.getPort() + ":" + predecessor.getIdentifier());
        try {
            Socket successorSocket = new Socket(successor.getHost(), successor.getPort());
            Socket predecessorSocket = new Socket(predecessor.getHost(), predecessor.getPort());
            Socket discoveryNodeSocket = new Socket(discoveryNodeHost, discoveryNodePort);
            sender.sendToSpecificSocket(successorSocket, leaving.getBytes());
            sender.sendToSpecificSocket(predecessorSocket, leaving.getBytes());
            sender.sendToSpecificSocket(discoveryNodeSocket, leaving.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
