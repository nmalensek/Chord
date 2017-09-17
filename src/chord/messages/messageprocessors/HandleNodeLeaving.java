package chord.messages.messageprocessors;

import chord.messages.AskForSuccessor;
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
    private HashMap<Integer, NodeRecord> knownNodes;
    private FingerTableManagement fingerTableManagement = new FingerTableManagement();

    public HandleNodeLeaving(String host, int port, int ID, Peer parent, HashMap<Integer, NodeRecord> knownNodes) {
        this.host = host;
        this.port = port;
        this.ID = ID;
        this.parent = parent;
        this.knownNodes = knownNodes;
    }

    public void processSuccessorLeaving(NodeLeaving leavingMessage) throws IOException {
        String[] successorInfo = (leavingMessage.getSuccessorInfo().split(":"));
        NodeRecord newSuccessor = new NodeRecord(
                successorInfo[0] + ":" + successorInfo[1],
                Integer.parseInt(successorInfo[2]),
                successorInfo[0],
                true
        );
        knownNodes.remove(leavingMessage.getSixteenBitID());
        knownNodes.putIfAbsent(newSuccessor.getIdentifier(), newSuccessor);
        AskForSuccessor askForSuccessor = new AskForSuccessor();
        sender.sendToSpecificSocket(newSuccessor.getNodeSocket(), askForSuccessor.getBytes());
    }

    public void processPredecessorLeaving(NodeLeaving leavingMessage) throws IOException {
        String[] predecessorInfo = (leavingMessage.getSuccessorInfo().split(":"));
        NodeRecord newPredecessor = new NodeRecord(
                predecessorInfo[0] + ":" + predecessorInfo[1],
                Integer.parseInt(predecessorInfo[2]),
                predecessorInfo[0],
                true
        );
        knownNodes.remove(leavingMessage.getSixteenBitID());
        knownNodes.putIfAbsent(newPredecessor.getIdentifier(), newPredecessor);
        parent.setPredecessor(newPredecessor);
        AskForSuccessor askForSuccessor = new AskForSuccessor();
        sender.sendToSpecificSocket(parent.getFingerTable().get(1).getNodeSocket(), askForSuccessor.getBytes());
    }

    public void removeDeadNodeAndUpdateFT(Socket errorSocket) { //double check this, may not be able to compare sockets
        for (NodeRecord node : knownNodes.values()) {
            if (node.getNodeSocket().equals(errorSocket)) {
                knownNodes.remove(node.getIdentifier());
            }
        }
        try {
            AskForSuccessor askForSuccessor = new AskForSuccessor();
            sender.sendToSpecificSocket(parent.getFingerTable().get(1).getNodeSocket(), askForSuccessor.getBytes());
        } catch (IOException e) {
            knownNodes.remove(parent.getFingerTable().get(1).getIdentifier()); //can't contact successor, find yourself again
            Lookup lookup = new Lookup();
            lookup.setPayloadID(ID);
            lookup.setRoutingPath(host + ":" + port + ",");
        }
    }

}
