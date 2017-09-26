package chord.messages.messageprocessors;

import chord.messages.AskForSuccessor;
import chord.messages.DeadNode;
import chord.messages.Lookup;
import chord.messages.NodeLeaving;
import chord.node.NodeRecord;
import chord.node.Peer;
import chord.transport.TCPSender;
import chord.util.FingerTableManagement;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class HandleNodeLeaving {

    private TCPSender sender = new TCPSender();
    private String host;
    private int port;
    private int ID;
    private Peer parent;
    private FingerTableManagement fingerTableManagement = new FingerTableManagement();

    public HandleNodeLeaving(String host, int port, int ID, Peer parent) {
        this.host = host;
        this.port = port;
        this.ID = ID;
        this.parent = parent;
    }

    public void processSuccessorLeaving(NodeLeaving leavingMessage) throws IOException {
        parent.getKnownNodes().remove(leavingMessage.getSixteenBitID());

        String[] newSuccessorInfo = (leavingMessage.getSuccessorInfo().split(":"));
        int newSuccessorID = Integer.parseInt(newSuccessorInfo[2]);

        if (parent.getKnownNodes().get(newSuccessorID) == null) {
                NodeRecord newSuccessor = new NodeRecord(
                        newSuccessorInfo[0] + ":" + newSuccessorInfo[1],
                        newSuccessorID, newSuccessorInfo[0],
                        new Socket(newSuccessorInfo[0], Integer.parseInt(newSuccessorInfo[1]))
                );

                parent.getKnownNodes().put(newSuccessor.getIdentifier(), newSuccessor);
        }
        fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());

        if (newSuccessorID != ID) {
            AskForSuccessor askForSuccessor = new AskForSuccessor();
            askForSuccessor.setOriginatorInformation(host + ":" + port + ":" + ID);
            sender.sendToSpecificSocket(parent.getKnownNodes().get(newSuccessorID).getNodeSocket(), askForSuccessor.getBytes());
        }
    }

    public void processPredecessorLeaving(NodeLeaving leavingMessage) throws IOException {
        parent.getKnownNodes().remove(leavingMessage.getSixteenBitID());

        String[] newPredecessorInfo = (leavingMessage.getPredecessorInfo().split(":"));
        int newPredecessorID = Integer.parseInt(newPredecessorInfo[2]);

        NodeRecord newPredecessor;
        if (parent.getKnownNodes().get(newPredecessorID) == null) {
            newPredecessor = new NodeRecord(
                    newPredecessorInfo[0] + ":" + newPredecessorInfo[1],
                    Integer.parseInt(newPredecessorInfo[2]),
                    newPredecessorInfo[0],
                    new Socket(newPredecessorInfo[0], Integer.parseInt(newPredecessorInfo[1]))
            );

            parent.getKnownNodes().put(newPredecessor.getIdentifier(), newPredecessor);
        } else {
            parent.setPredecessor(parent.getKnownNodes().get(newPredecessorID));
        }
        if (newPredecessorID != ID) {
//            AskForSuccessor askForSuccessor = new AskForSuccessor(); //this node is successor of new predecessor, so traverse the ring
//            askForSuccessor.setOriginatorInformation(host + ":" + port + ":" + ID);
//            sender.sendToSpecificSocket(parent.getFingerTable().get(1).getNodeSocket(), askForSuccessor.getBytes());
            DeadNode deadNode = new DeadNode();
            deadNode.setDeadNodeID(leavingMessage.getSixteenBitID());
            sender.sendToSpecificSocket(parent.getFingerTable().get(1).getNodeSocket(), deadNode.getBytes());
        }
        fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
    }

    public synchronized void removeDeadNodeUpdateFTAndForward(DeadNode deadNode) throws IOException {
        NodeRecord attemptedRemoval = null;
        attemptedRemoval = parent.getKnownNodes().remove(deadNode.getDeadNodeID());
        if (attemptedRemoval != null) {
            fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
            sender.sendToSpecificSocket(parent.getFingerTable().get(1).getNodeSocket(), deadNode.getBytes());
        }
    }

}
