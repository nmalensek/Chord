package chord.util;

import chord.messages.NodeLeaving;
import chord.node.NodeRecord;
import chord.node.Peer;
import chord.transport.TCPSender;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ShutdownHook extends Thread {

    private TCPSender sender;
    private Peer owner;
    private int ownerNodeID;
    private Socket discoveryNodeSocket;
    private Thread mainThread;
//    private ArrayList<Thread> threadsToInterrupt = new ArrayList<>();

    public ShutdownHook(TCPSender sender, Peer owner, int ownerNodeID,
                        Socket discoveryNodeSocket, Thread mainThread) {
        this.sender = sender;
        this.ownerNodeID = ownerNodeID;
        this.owner = owner;
        this.discoveryNodeSocket = discoveryNodeSocket;
        this.mainThread = mainThread;
    }

//    public void addThreadToInterrupt(Thread thread) {
//        threadsToInterrupt.add(thread);
//    }

    @Override
    public void run() {
//        try {
//            running = false;
//            mainThread.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        for (Thread aliveThread : threadsToInterrupt) {
//            aliveThread.interrupt();
//        }
        NodeRecord successor = owner.getFingerTable().get(1);
        NodeRecord predecessor = owner.getPredecessor();

        NodeLeaving leaving = new NodeLeaving();
        leaving.setSixteenBitID(ownerNodeID);
        leaving.setSuccessorInfo(successor.toString());
        leaving.setPredecessorInfo(predecessor.toString());
        try {
            if (successor.getNodeSocket() != null) {
                owner.sendFilesToSuccessor();
                sender.sendToSpecificSocket(successor.getNodeSocket(), leaving.getBytes());
                sender.sendToSpecificSocket(predecessor.getNodeSocket(), leaving.getBytes());
            }
            sender.sendToSpecificSocket(discoveryNodeSocket, leaving.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
