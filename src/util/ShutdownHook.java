package util;

import messaging.NodeLeaving;
import node.NodeRecord;
import transport.TCPSender;

import java.io.IOException;
import java.net.Socket;

public class ShutdownHook extends Thread {

    private TCPSender sender;
    private NodeRecord successor;
    private NodeRecord predecessor;
    private String ownerNodeID;
    private String discoveryNodeHost;
    private int discoveryNodePort;

    public ShutdownHook(TCPSender sender, NodeRecord successor, NodeRecord predecessor,
                        String ownerNodeID, String discoveryNodeHost, int discoveryNodePort) {
        this.sender = sender;
        this.successor = successor;
        this.predecessor = predecessor;
        this.ownerNodeID = ownerNodeID;
        this.discoveryNodeHost = discoveryNodeHost;
        this. discoveryNodePort = discoveryNodePort;
    }

    @Override
    public void run() {
        NodeLeaving leaving = new NodeLeaving();
        leaving.setSixteenBitID(ownerNodeID);
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
