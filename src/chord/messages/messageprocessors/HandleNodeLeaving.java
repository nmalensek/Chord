package chord.messages.messageprocessors;

import chord.messages.AskForSuccessor;
import chord.messages.DeadNode;
import chord.messages.NodeLeaving;
import chord.node.NodeRecord;
import chord.node.Peer;
import chord.transport.TCPSender;
import chord.util.FingerTableManagement;

import java.io.IOException;
import java.net.Socket;

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

    /**
     * Successor left, so either add new successor to known nodes and update finger table
     * or just update the finger table. Removes node that's leaving from known nodes.
     * After updating, the node asks all other nodes in the ring who their successor is
     * to make sure it still has the most current information.
     * @param leavingMessage message containing leaving node's information.
     * @throws IOException
     */
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

    /**
     * Either registers new node as predecessor and updates finger table or just sets the
     * known node as its predecessors and updates finger table. Node then sends information
     * about the dead node to its successor, since it otherwise might not have known about
     * the node dying. Finger tables are aggressively maintained as a result.
     * @param leavingMessage message from leaving node.
     * @throws IOException
     */
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
            DeadNode deadNode = new DeadNode();
            deadNode.setDeadNodeID(leavingMessage.getSixteenBitID());
            sender.sendToSpecificSocket(parent.getFingerTable().get(1).getNodeSocket(), deadNode.getBytes());
        }
        fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
    }

    /**
     * If a node receives a notification that a node died, it removes the node from its finger table
     * and then forwards information about that node to its successor so all nodes know if a node
     * leaves the overlay.
     * @param deadNode node that left the overlay.
     * @throws IOException
     */
    public synchronized void removeDeadNodeUpdateFTAndForward(DeadNode deadNode) throws IOException {
        NodeRecord attemptedRemoval = null;
        attemptedRemoval = parent.getKnownNodes().remove(deadNode.getDeadNodeID());
        if (attemptedRemoval != null) {
            fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
            sender.sendToSpecificSocket(parent.getFingerTable().get(1).getNodeSocket(), deadNode.getBytes());
        }
    }

}
