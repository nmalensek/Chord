package chord.messages.messageprocessors;

import chord.messages.*;
import chord.node.NodeRecord;
import chord.node.Peer;
import chord.transport.TCPSender;
import chord.util.FingerTableManagement;
import chord.util.SplitHostPort;

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
    private FingerTableManagement fingerTableManagement = new FingerTableManagement();
    private SplitHostPort split = new SplitHostPort();
    private Socket parentSocket;


    public MessageProcessor(String host, int port, int ID,
                            Peer parent, Socket parentSocket) throws IOException {
        this.host = host;
        this.port = port;
        this.ID = ID;
        this.parent = parent;
        this.parentSocket = parentSocket;
        self = new NodeRecord(host + ":" + port, ID, host, parentSocket);
        parent.getKnownNodes().put(ID, self);
        parent.setPredecessor(self);
    }

    public synchronized void processRegistration(NodeInformation information) throws IOException {
        if (information.getSixteenBitID() != ID) { //not first node in the ring so find out where to go
            Lookup findLocation = new Lookup();
            findLocation.setRoutingPath(self.toString() + ",");
            findLocation.setPayloadID(ID);
            Socket randomNodeSocket = new Socket(split.getHost(information.getHostPort()),
                    split.getPort(information.getHostPort()));
            sender.sendToSpecificSocket(randomNodeSocket, findLocation.getBytes());
            parent.getKnownNodes().put(information.getSixteenBitID(),
                    new NodeRecord(information.getHostPort(),
                            information.getSixteenBitID(),
                            information.getNickname(), randomNodeSocket));
        }
    }

    public synchronized void processLookup(Lookup lookupEvent, NodeRecord predecessor) throws IOException {
        int payload = lookupEvent.getPayloadID();
        int predecessorID = predecessor.getIdentifier();
        NodeRecord successor = parent.getFingerTable().get(1);
        if (payload > predecessorID && payload <= ID) { //normal message storage condition
            sendDestinationMessage(lookupEvent, self.toString(), predecessor.toString());
        } else if (predecessorID == ID) { //second node joins overlay
            sendDestinationMessage(lookupEvent, self.toString(), self.toString());
        } else if (predecessorID == successor.getIdentifier()) { //third node joins overlay
            NodeRecord larger = ID > predecessorID ? self : predecessor;
            NodeRecord smaller = ID < predecessorID ? self : predecessor;
            if (payload > larger.getIdentifier() || payload < smaller.getIdentifier()) {
                sendDestinationMessage(lookupEvent, smaller.toString(), larger.toString());
            } else if (smaller.getIdentifier() < payload && payload < larger.getIdentifier()) {
                sendDestinationMessage(lookupEvent, larger.toString(), smaller.toString());
            }
        } else if (successor.getIdentifier() >= payload && predecessorID < payload) { //Successor is largest, send to there
            forwardLookup(lookupEvent, successor);
        } else if (successor.getIdentifier() >= payload && fingerTableManagement.predecessorIsLargest(parent.getKnownNodes(), predecessorID)) {
            forwardLookup(lookupEvent, successor); //send to successor if predecessor's the largest node in the overlay
        }
        else {
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

    private synchronized void sendDestinationMessage(Lookup lookup, String successorHostPortID, String predecessorHostPortID) throws IOException {
        DestinationNode thisNodeIsSink = new DestinationNode();
        thisNodeIsSink.setDestinationNode(successorHostPortID);
        thisNodeIsSink.setDestinationPredecessor(predecessorHostPortID);
        System.out.println("sent destination message with predecessor information of "
        + thisNodeIsSink.getDestinationPredecessor() + "\nand successor information: " + thisNodeIsSink.getDestinationNode());

        String originatingNode = lookup.getRoutingPath().split(",")[0];
        String originatingHost = split.getHost(originatingNode);
        int originatingPort = split.getPort(originatingNode);
        int originatingID = split.getID(originatingNode);
        Socket requestorSocket = checkIfKnown(originatingHost, originatingPort, originatingID);

        sender.sendToSpecificSocket(requestorSocket, thisNodeIsSink.getBytes());
        System.out.println("Routing: " + lookup.getRoutingPath() + " " + self.toString());
        System.out.println("Hops: " + (lookup.getNumHops() + 1));
    }

    private synchronized Socket checkIfKnown(String originatingHost, int originatingPort, int originatingID) throws IOException {
        Socket socket;
        if (parent.getKnownNodes().get(originatingID) == null) {
            socket = new Socket(originatingHost, originatingPort);
            NodeRecord node = new NodeRecord(originatingHost + ":" + originatingPort, originatingID, originatingHost, socket);
            parent.getKnownNodes().put(originatingID, node);
            fingerTableManagement.updateFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
        } else {
            socket = parent.getKnownNodes().get(originatingID).getNodeSocket();
        }
        return socket;
    }

    private synchronized void forwardLookup(Lookup lookup, NodeRecord forwardTarget) throws IOException {
        lookup.setRoutingPath(lookup.getRoutingPath() + self.toString() + ",");
        lookup.setNumHops((lookup.getNumHops() + 1));
        Socket successorSocket = forwardTarget.getNodeSocket();
        System.out.println("Forwarding lookup message to " + forwardTarget);
        sender.sendToSpecificSocket(successorSocket, lookup.getBytes());
        System.out.println("Hops: " + lookup.getNumHops() + "\tfor id: " + lookup.getPayloadID());
    }

    public synchronized void processDestination(DestinationNode destinationNode) throws IOException {
        //a destination means that node's your successor, so set successor
        String successorHostPortID = destinationNode.getDestinationNode();
        int successorID = split.getID(successorHostPortID);
        String successorNickname = split.getHost(successorHostPortID);
        Socket successorSocket =
                new Socket(split.getHost(successorHostPortID), split.getPort(successorHostPortID));

        NodeRecord successorNode = new NodeRecord(split.getHostPort(successorHostPortID), successorID, successorNickname, successorSocket);
        parent.getFingerTable().put(1, successorNode);
        parent.getKnownNodes().put(successorID, successorNode);

        String predecessorInfo = destinationNode.getDestinationPredecessor();
        if (parent.getKnownNodes().get(split.getID(predecessorInfo)) == null) {
            Socket predecessorSocket = new Socket(split.getHost(predecessorInfo), split.getPort(predecessorInfo));
            String predHost = split.getHost(predecessorInfo);
            int predPort = split.getPort(predecessorInfo);
            int predID = split.getID(predecessorInfo);
            NodeRecord newPredecessor = new NodeRecord(predHost + ":" + predPort, predID, predHost, predecessorSocket);
            parent.getKnownNodes().put(predID, newPredecessor);
            parent.setPredecessor(newPredecessor);
        } else {
            parent.setPredecessor(parent.getKnownNodes().get(split.getID(predecessorInfo)));
        }

        UpdatePredecessor updatePredecessor = new UpdatePredecessor(); //tell successor to update its predecessor
        updatePredecessor.setPredecessorInfo(host + ":" + port + ":" + ID);
        sender.sendToSpecificSocket(successorNode.getNodeSocket(), updatePredecessor.getBytes());

        fingerTableManagement.updateFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
        fingerTableManagement.printFingerTable(parent.getFingerTable());
    }

    public synchronized void processPredecessorUpdate(UpdatePredecessor updatePredecessor, HashMap<Integer, String> fileHashMap) throws IOException {
        int predecessorID = split.getID(updatePredecessor.getPredecessorInfo());
        String predecessorHost = split.getHost(updatePredecessor.getPredecessorInfo());
        int predecessorPort = split.getPort(updatePredecessor.getPredecessorInfo());
        String predecessorHostPort = split.getHostPort(updatePredecessor.getPredecessorInfo());

        Socket predecessorSocket = new Socket(predecessorHost, predecessorPort);
        NodeRecord newPredecessor =
                new NodeRecord(predecessorHostPort, predecessorID, predecessorHost, predecessorSocket);

        parent.setPredecessor(newPredecessor);
        parent.getKnownNodes().putIfAbsent(predecessorID, newPredecessor);

        fingerTableManagement.updateFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
        parent.setFingerTableModified(true);

//        if (parent.getKnownNodes().size() > 3) {
//            NodeRecord successor = parent.getFingerTable().get(1); //tell new predecessor about your successor
//            sendSuccessorInformation(successor, newPredecessor.getNodeSocket());
//        }
//
//        int filesTransferred = 0;
//        for (int fileKey : fileHashMap.keySet()) {
//            if (fileKey <= newPredecessorID) {
//                FilePayload file = new FilePayload();
//                file.setFileID(fileKey);
//                file.setFileName(fileHashMap.get(fileKey).substring(5, fileHashMap.get(fileKey).length()));
//                file.setFileToTransfer(new File(fileHashMap.get(fileKey)));
//                System.out.println("Sending file " + fileKey + " to " + newPredecessorHostPort + "\tID: "
//                        + newPredecessorID);
//                TCPSender sender = new TCPSender();
//                sender.sendToSpecificSocket(newPredecessor.getNodeSocket(), file.getBytes());
//                filesTransferred++;
//            }
//            if (filesTransferred > 0) {
//                parent.setFilesResponsibleForModified(true);
//            }
//        }
    }

    public synchronized void sendSuccessorInformation(NodeRecord successor, Socket askerSocket) throws IOException {
        SuccessorInformation successorInformation = new SuccessorInformation();
        successorInformation.setSuccessorHostPort(successor.getHost() + ":" + successor.getPort());
        successorInformation.setSuccessorID(successor.getIdentifier());
        successorInformation.setSuccessorNickname(successor.getNickname());
        sender.sendToSpecificSocket(askerSocket, successorInformation.getBytes());
    }

//    public synchronized void processSuccessorInformation(SuccessorInformation successorInformation) throws IOException {
//        String successorHostPort = successorInformation.getSuccessorHostPort();
//
//        if (!split.getHost(successorHostPort).equals(host) &&
//                split.getPort(successorHostPort) != port) { //stop if node's successor is this node
//
//            int successorID = successorInformation.getSuccessorID();
//            String successorNickname = successorInformation.getSuccessorNickname();
//
//            NodeRecord successorNode;
//            if (parent.getKnownNodes().get(successorID) == null) {
//                Socket successorSocket = new Socket(split.getHost(successorHostPort),
//                        split.getPort(successorHostPort));
//                successorNode = new NodeRecord(successorHostPort, successorID, successorNickname, successorSocket);
//            } else {
//                successorNode = parent.getKnownNodes().get(successorID);
//            }
//
//            AskForSuccessor askForSuccessor = new AskForSuccessor();
//            askForSuccessor.setOriginatorInformation(host + ":" + port + ":" + ID);
//            sender.sendToSpecificSocket(successorNode.getNodeSocket(), askForSuccessor.getBytes());
//        } else {
//            fingerTableManagement.updateFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
//            parent.setFingerTableModified(true);
//        }
//    }

//    public synchronized void checkIfUnknownNode(AskForSuccessor requestForSuccessor) throws IOException {
//        String[] requestOriginator = requestForSuccessor.getOriginatorInformation().split(":");
//        int originatorID = Integer.parseInt(requestOriginator[2]);
//        String originatorHostPort = requestOriginator[0] + ":" + requestOriginator[1];
//        if (parent.getKnownNodes().get(originatorID) == null) {
//            parent.getKnownNodes().put(originatorID, new NodeRecord(originatorHostPort, originatorID,
//                    requestOriginator[0],new Socket(requestOriginator[0], split.getPort(originatorHostPort))));
//            fingerTableManagement.updateFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
//            parent.setFingerTableModified(true);
//        }
//    }
//
//    public synchronized void processPayload(FilePayload filePayload, HashMap<Integer, String> filesResponsibleFor) throws IOException {
//        if (filesResponsibleFor.get(filePayload.getFileID()) != null) {
//            filePayload.writeFile(filePayload.getFileByteArray(), "/tmp/" + filePayload.getFileName());
//            synchronized (filesResponsibleFor) {
//                filesResponsibleFor.put(filePayload.getFileID(), "/tmp/" + filePayload.getFileName());
//            }
//        } else {
//            Collision collision = new Collision();
//            String senderHost = filePayload.getSendingNodeHostPort().split(":")[0];
//            int senderPort = Integer.parseInt(filePayload.getSendingNodeHostPort().split(":")[1]);
//            Socket fileOwnerSocket = new Socket(senderHost, senderPort);
//            sender.sendToSpecificSocket(fileOwnerSocket, collision.getBytes());
//        }
//    }
}
