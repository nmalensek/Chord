package chord.messages.messageprocessors;

import chord.messages.*;
import chord.node.NodeRecord;
import chord.node.Peer;
import chord.transport.TCPSender;
import chord.util.FingerTableManagement;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class MessageProcessor {

    private TCPSender sender = new TCPSender();
    private String host;
    private int port;
    private int ID;
    private Peer parent;
    private NodeRecord self;
    private HashMap<Integer, NodeRecord> knownNodes;
    private FingerTableManagement fingerTableManagement = new FingerTableManagement();


    public MessageProcessor(String host, int port, int ID, Peer parent, HashMap<Integer, NodeRecord> knownNodes) throws IOException {
        this.host = host;
        this.port = port;
        this.ID = ID;
        this.parent = parent;
        this.knownNodes = knownNodes;
        self = new NodeRecord(host + ":" + port, ID, host, false);
        knownNodes.put(ID, self);
    }

    public void processRegistration(NodeInformation information) throws IOException {
        if (information.getSixteenBitID() != ID) { //not first node in the ring so find out where to go
            Lookup findLocation = new Lookup();
            findLocation.setRoutingPath(host + ":" + port + ",");
            findLocation.setPayloadID(ID);
            Socket randomNodeSocket = new Socket(information.getHostPort().split(":")[0],
                    Integer.parseInt(information.getHostPort().split(":")[1]));
            sender.sendToSpecificSocket(randomNodeSocket, findLocation.getBytes());
            knownNodes.put(information.getSixteenBitID(),
                    new NodeRecord(information.getHostPort(),
                            information.getSixteenBitID(),
                            information.getNickname(), true));
        } else {
            parent.setPredecessor(self);
        }
    }

    public void processLookup(Lookup lookupEvent, NodeRecord predecessor) throws IOException {
        int payload = lookupEvent.getPayloadID();
        int predecessorID = predecessor.getIdentifier();
        NodeRecord successor = parent.getFingerTable().get(1);
        if (payload > predecessorID && payload <= ID) { //normal message storage condition
            sendDestinationMessage(lookupEvent, host + ":" + port);
        } else if (predecessorID == ID) { //second node joins overlay
            sendDestinationMessage(lookupEvent, host + ":" + port);
        } else if (predecessorID == successor.getIdentifier()) { //third node joins overlay
            NodeRecord larger = ID > predecessorID ? self : predecessor;
            NodeRecord smaller = ID < predecessorID ? self : predecessor;
            if (payload > larger.getIdentifier() || payload < smaller.getIdentifier()) {
                sendDestinationMessage(lookupEvent, smaller.getHost() + ":" + smaller.getPort());
            } else if (smaller.getIdentifier() < payload && payload < larger.getIdentifier()) {
                sendDestinationMessage(lookupEvent, larger.getHost() + ":" + larger.getPort());
            }
        } else if (successor.getIdentifier() > payload && predecessorID < payload) { //Successor is largest, send to there
            forwardLookup(lookupEvent, successor);
        } else {
            HashMap<Integer, NodeRecord> parentFingerTable = parent.getFingerTable();
            for (int key : parentFingerTable.keySet()) {
                NodeRecord currentRow = parentFingerTable.get(key);
                NodeRecord nextRow = parentFingerTable.get(key + 1);
                if (nextRow == null) {
                    forwardLookup(lookupEvent, currentRow); //you're at the last FT entry, forward to currentRow
                    break;
                } else if (currentRow.getIdentifier() <= payload && payload < nextRow.getIdentifier()) {
                    forwardLookup(lookupEvent, currentRow); //currentRow is largest FT row that's still less than k, send to there
                    break;
                }
            }
        }
    }

    private void sendDestinationMessage(Lookup lookup, String destinationNodeHostPort) throws IOException {
        DestinationNode thisNodeIsSink = new DestinationNode();
        thisNodeIsSink.setHostPort(destinationNodeHostPort);
        String originatingNode = lookup.getRoutingPath().split(",")[0];
        String originatingHost = originatingNode.split(":")[0];
        int originatingPort = Integer.parseInt(originatingNode.split(":")[1]);
        Socket requestorSocket = new Socket(originatingHost, originatingPort);
        sender.sendToSpecificSocket(requestorSocket, thisNodeIsSink.getBytes());
        System.out.println("Routing: " + lookup.getRoutingPath() + "," + host + ":" + port);
        System.out.println("Hops: " + (lookup.getNumHops() + 1));
    }

    private void forwardLookup(Lookup lookup, NodeRecord forwardTarget) throws IOException {
        lookup.setRoutingPath(lookup.getRoutingPath() + host + ":" + port + ",");
        lookup.setNumHops((lookup.getNumHops() + 1));
        Socket successorSocket = new Socket(forwardTarget.getHost(), forwardTarget.getPort());
        sender.sendToSpecificSocket(successorSocket, lookup.getBytes());
        System.out.println("Hops: " + lookup.getNumHops() + "\tfor id: " + lookup.getPayloadID());
    }

    public void processDestination(DestinationNode destinationNode) throws IOException {
        //a destination means that node's your successor, so set successor
        String successorHostPort = destinationNode.getHostPort();
        int successorID = destinationNode.getDestinationID();
        String successorNickname = destinationNode.getDestinationNickname();
        NodeRecord successorNode = new NodeRecord(successorHostPort, successorID, successorNickname, true);
        parent.getFingerTable().put(1, successorNode);

        //TODO Add successor's predecessor information to destination message

        UpdatePredecessor updatePredecessor = new UpdatePredecessor(); //tell successor to update its predecessor
        updatePredecessor.setPredecessorID(ID);
        updatePredecessor.setPredecessorHostPort(host + ":" + port);
        updatePredecessor.setPredecessorNickname(host);
        sender.sendToSpecificSocket(successorNode.getNodeSocket(), updatePredecessor.getBytes());

        knownNodes.putIfAbsent(successorID, successorNode);
    }

    public void processPredecessorUpdate(UpdatePredecessor updatePredecessor, HashMap<Integer, String> fileHashMap) throws IOException {
        String newPredecessorHostPort = updatePredecessor.getPredecessorHostPort();
        int newPredecessorID = updatePredecessor.getPredecessorID();
        String newPredecessorNickname = updatePredecessor.getPredecessorNickname();
        NodeRecord newPredecessor =
                new NodeRecord(newPredecessorHostPort, newPredecessorID, newPredecessorNickname, true);
        parent.setPredecessor(newPredecessor);
        knownNodes.putIfAbsent(newPredecessorID, newPredecessor);

        fingerTableManagement.updateFingerTable(ID, parent.getFingerTable(), knownNodes);
        parent.setFingerTableModified(true);

        NodeRecord successor = parent.getFingerTable().get(1); //tell new predecessor about your successor
        sendSuccessorInformation(successor, newPredecessor.getNodeSocket());

        for (int fileKey : fileHashMap.keySet()) {
            if (fileKey <= newPredecessorID) {
                FilePayload file = new FilePayload();
                file.setFileID(fileKey);
                file.setFileName(fileHashMap.get(fileKey).substring(5, fileHashMap.get(fileKey).length()));
                file.setFileToTransfer(new File(fileHashMap.get(fileKey)));
                System.out.println("Sending file " + fileKey + " to " + newPredecessorHostPort + "\tID: "
                        + newPredecessorID);
                TCPSender sender = new TCPSender();
                sender.sendToSpecificSocket(newPredecessor.getNodeSocket(), file.getBytes());
            }
        }
        parent.setFilesResponsibleForModified(true);

    }

    public void sendSuccessorInformation(NodeRecord successor, Socket askerSocket) throws IOException {
        SuccessorInformation successorInformation = new SuccessorInformation();
        successorInformation.setSuccessorHostPort(successor.getHost() + ":" + successor.getPort());
        successorInformation.setSuccessorID(successor.getIdentifier());
        successorInformation.setSuccessorNickname(successor.getNickname());
        sender.sendToSpecificSocket(askerSocket, successorInformation.getBytes());
    }

    public void processSuccessorInformation(SuccessorInformation successorInformation) throws IOException {
        String successorHostPort = successorInformation.getSuccessorHostPort();

        if (!successorHostPort.split(":")[0].equals(host) &&
                Integer.parseInt(successorHostPort.split(":")[1]) != port) { //stop if node's successor is this node

            int successorID = successorInformation.getSuccessorID();
            String successorNickname = successorInformation.getSuccessorNickname();

            NodeRecord successorNode = new NodeRecord(successorHostPort, successorID, successorNickname, true);

            knownNodes.putIfAbsent(successorID, successorNode);

            AskForSuccessor askForSuccessor = new AskForSuccessor();
            askForSuccessor.setOriginatorInformation(host + ":" + port + ":" + ID);
            sender.sendToSpecificSocket(successorNode.getNodeSocket(), askForSuccessor.getBytes());
        } else {
            fingerTableManagement.updateFingerTable(ID, parent.getFingerTable(), knownNodes);
            parent.setFingerTableModified(true);
        }
    }

    public void checkIfUnknownNode(AskForSuccessor requestForSuccessor) throws IOException {
        String[] requestOriginator = requestForSuccessor.getOriginatorInformation().split(":");
        int originatorID = Integer.parseInt(requestOriginator[2]);
        String originatorHostPort = requestOriginator[0] + ":" + requestOriginator[1];
        if (knownNodes.get(originatorID) == null) {
            knownNodes.put(originatorID, new NodeRecord(originatorHostPort, originatorID, requestOriginator[0],true));
            fingerTableManagement.updateFingerTable(ID, parent.getFingerTable(), knownNodes);
            parent.setFingerTableModified(true);
        }
    }

    public void processPayload(FilePayload filePayload, HashMap<Integer, String> filesResponsibleFor) throws IOException {
        if (filesResponsibleFor.get(filePayload.getFileID()) != null) {
            filePayload.writeFile(filePayload.getFileByteArray(), "/tmp/" + filePayload.getFileName());
            synchronized (filesResponsibleFor) {
                filesResponsibleFor.put(filePayload.getFileID(), "/tmp/" + filePayload.getFileName());
            }
        } else {
            Collision collision = new Collision();
            String senderHost = filePayload.getSendingNodeHostPort().split(":")[0];
            int senderPort = Integer.parseInt(filePayload.getSendingNodeHostPort().split(":")[1]);
            Socket fileOwnerSocket = new Socket(senderHost, senderPort);
            sender.sendToSpecificSocket(fileOwnerSocket, collision.getBytes());
        }
    }
}
