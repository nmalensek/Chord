package chord.messages.messageprocessors;

import chord.messages.*;
import chord.node.NodeRecord;
import chord.node.Peer;
import chord.transport.TCPSender;
import chord.util.FingerTableManagement;
import chord.util.SplitHostPort;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

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
    private String storageDirectory = "/tmp/";
    private static int storeDataID = 99999;
    private Socket storeDataSocket;


    public MessageProcessor(String host, int port, int ID,
                            Peer parent, Socket parentSocket, Socket storeDataSocket) throws IOException {
        this.host = host;
        this.port = port;
        this.ID = ID;
        this.parent = parent;
        this.parentSocket = parentSocket;
        this.storeDataSocket = storeDataSocket;
        self = new NodeRecord(host + ":" + port, ID, host, parentSocket);
        parent.getKnownNodes().put(ID, self);
        parent.setPredecessor(self);
    }

    public synchronized void processRegistration(NodeInformation information) throws IOException {
        int newNodeID = split.getID(information.getNodeInfo());
        if (newNodeID != ID) { //not first node in the ring so find out where to go
            Lookup findLocation = new Lookup();
            findLocation.setRoutingPath(self.toString() + ",");
            findLocation.setPayloadID(ID);
            Socket randomNodeSocket = new Socket(split.getHost(information.getNodeInfo()),
                    split.getPort(information.getNodeInfo()));
            sender.sendToSpecificSocket(randomNodeSocket, findLocation.getBytes());
            parent.getKnownNodes().put(newNodeID,
                    new NodeRecord(split.getHostPort(information.getNodeInfo()),
                            newNodeID,
                            split.getHost(information.getNodeInfo()), randomNodeSocket));
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
        } else if (fingerTableManagement.nodeIsSmallestConcurrent(parent.getKnownNodes(), ID)
                && payload < ID && predecessorID > ID) {
            sendDestinationMessage(lookupEvent, self.toString(), predecessor.toString());
        } else if (successor.getIdentifier() >= payload && predecessorID < payload) { //Successor is largest, send to there
            forwardLookup(lookupEvent, successor);
        } else if (successor.getIdentifier() >= payload &&
                fingerTableManagement.nodeIsLargestConcurrent(parent.getKnownNodes(), predecessorID)) {
            forwardLookup(lookupEvent, successor); //send to successor if predecessor's the largest node in the overlay
        } else if (fingerTableManagement.nodeIsLargestConcurrent(parent.getKnownNodes(), ID) && payload > ID) {
            sendDestinationMessage(lookupEvent, successor.toString(), self.toString());
        }
        else {
            ConcurrentHashMap<Integer, NodeRecord> parentFingerTable = parent.getFingerTable();
            for (int key : parentFingerTable.keySet()) {
                NodeRecord currentRow = parentFingerTable.get(key);
                NodeRecord nextRow = parentFingerTable.get(key + 1);
                if (nextRow == null) {
                    if (currentRow.getIdentifier() == ID) { //if the last row is yourself, you're the destination
                        sendDestinationMessage(lookupEvent, self.toString(), parent.getPredecessor().toString());
                    } else {
                        forwardLookup(lookupEvent, currentRow); //you're at the last FT entry, forward to currentRow
                    }
                    break;
                } else if (currentRow.getIdentifier() <= payload && payload < nextRow.getIdentifier()) {
                    forwardLookup(lookupEvent, currentRow); //currentRow is largest FT row that's still less than k, send to there
                    break;
                }
            }
        }
    }

    private synchronized void sendDestinationMessage(Lookup lookup, String successorHostPortID,
                                                     String predecessorHostPortID) throws IOException {
        DestinationNode thisNodeIsSink = new DestinationNode();
        thisNodeIsSink.setDestinationNode(successorHostPortID);
        thisNodeIsSink.setDestinationPredecessor(predecessorHostPortID);
        thisNodeIsSink.setOrigin(self.toString());
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
        if (parent.getKnownNodes().get(originatingID) == null && originatingID != storeDataID) {
            socket = new Socket(originatingHost, originatingPort);
            NodeRecord node = new NodeRecord(originatingHost + ":" + originatingPort, originatingID, originatingHost, socket);
            parent.getKnownNodes().put(originatingID, node);
            fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
        } else if (originatingID == storeDataID) {
            socket = storeDataSocket;
        } else {
            socket = parent.getKnownNodes().get(originatingID).getNodeSocket();
        }
        return socket;
    }

    private synchronized void forwardLookup(Lookup lookup, NodeRecord forwardTarget) throws IOException {
        try {
            lookup.setRoutingPath(lookup.getRoutingPath() + self.toString() + ",");
            Socket targetNodeSocket = forwardTarget.getNodeSocket();
            lookup.setNumHops((lookup.getNumHops() + 1));
            sender.sendToSpecificSocket(targetNodeSocket, lookup.getBytes());
            System.out.println("Forwarding lookup message to " + forwardTarget);
            System.out.println("Hops: " + lookup.getNumHops() + "\tfor id: " + lookup.getPayloadID());
        } catch (NullPointerException npe) {
            lookup.setNumHops((lookup.getNumHops() - 1));
            parent.getKnownNodes().remove(forwardTarget.getIdentifier());
            fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
            processLookup(lookup, parent.getPredecessor());
        }

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

        fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
        fingerTableManagement.printConcurrentFingerTable(parent.getFingerTable());


    }

    public synchronized void processPredecessorUpdate(UpdatePredecessor updatePredecessor,
                                                      ConcurrentHashMap<Integer, File> fileHashMap) throws IOException {
        int predecessorID = split.getID(updatePredecessor.getPredecessorInfo());
        String predecessorHost = split.getHost(updatePredecessor.getPredecessorInfo());
        int predecessorPort = split.getPort(updatePredecessor.getPredecessorInfo());
        String predecessorHostPort = split.getHostPort(updatePredecessor.getPredecessorInfo());

        Socket predecessorSocket = new Socket(predecessorHost, predecessorPort);
        NodeRecord newPredecessor =
                new NodeRecord(predecessorHostPort, predecessorID, predecessorHost, predecessorSocket);

        parent.setPredecessor(newPredecessor);
        parent.getKnownNodes().putIfAbsent(predecessorID, newPredecessor);

        fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
        parent.setFingerTableModified(true);

        if (parent.getKnownNodes().size() > 2) {
            NodeRecord successor = parent.getFingerTable().get(1); //tell new predecessor about your successor
            sendSuccessorInformation(successor, null, newPredecessor.getNodeSocket());
        }

        int filesTransferred = 0;
        for (int fileKey : fileHashMap.keySet()) {
            if (predecessorID < ID && predecessorID >= fileKey || predecessorID > ID && predecessorID >= fileKey && fileKey > ID) {
                FilePayload file = new FilePayload();

                file.setFileID(fileKey);
                file.setFileName(fileHashMap.get(fileKey).getName());
                file.setFileToTransfer(fileHashMap.get(fileKey));
                file.setSendingNodeHostPort(host + ":" + port);
                System.out.println("Sending file " + fileKey + " to " + newPredecessor.toString());
                TCPSender sender = new TCPSender();
                sender.sendToSpecificSocket(newPredecessor.getNodeSocket(), file.getBytes());

                File transferredFile = fileHashMap.remove(fileKey);
                Files.delete(Paths.get(transferredFile.getPath()));

                filesTransferred++;
            }
            if (filesTransferred > 0) {
                parent.setFilesResponsibleForModified(true);
            }
        }
    }

    public synchronized void sendSuccessorInformation(NodeRecord successor, AskForSuccessor message,
                                                      Socket destinationSocket) throws IOException {
        SuccessorInformation successorInformation = new SuccessorInformation();
        successorInformation.setSuccessorInfo(successor.toString());

        if (destinationSocket == null) {
            System.out.println("sending on ask for successor message");
            String senderHost = split.getHost(message.getOriginatorInformation());
            int senderPort = split.getPort(message.getOriginatorInformation());
            Socket senderSocket = new Socket(senderHost, senderPort);
            sender.sendToSpecificSocket(senderSocket, successorInformation.getBytes());
        } else {
            System.out.println("sending on original join");
            sender.sendToSpecificSocket(destinationSocket, successorInformation.getBytes());
        }
    }

    public synchronized void processSuccessorInformation(SuccessorInformation successorInformation) throws IOException {
        String successorHostPort = split.getHostPort(successorInformation.getSuccessorInfo());
        int successorID = split.getID(successorInformation.getSuccessorInfo());

        if (!split.getHost(successorHostPort).equals(host) ||
                split.getPort(successorHostPort) != port) { //stop if node's successor is this node
            String successorNickname = split.getHost(successorInformation.getSuccessorInfo());

            NodeRecord successorNode;
            if (parent.getKnownNodes().get(successorID) == null) {
                System.out.println("ADDING NEW NODE INFORMATION");
                Socket successorSocket = new Socket(split.getHost(successorHostPort),
                        split.getPort(successorHostPort));
                successorNode = new NodeRecord(successorHostPort, successorID, successorNickname, successorSocket);
                parent.getKnownNodes().put(successorID, successorNode);
            } else {
                successorNode = parent.getKnownNodes().get(successorID);
            }

            AskForSuccessor askForSuccessor = new AskForSuccessor();
            askForSuccessor.setOriginatorInformation(host + ":" + port + ":" + ID);
            sender.sendToSpecificSocket(successorNode.getNodeSocket(), askForSuccessor.getBytes());
        } else {
            fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
            parent.setFingerTableModified(true);
        }
    }

    public synchronized void checkIfUnknownNode(AskForSuccessor requestForSuccessor) throws IOException {
        String[] requestOriginator = requestForSuccessor.getOriginatorInformation().split(":");
        int originatorID = Integer.parseInt(requestOriginator[2]);
        String originatorHostPort = requestOriginator[0] + ":" + requestOriginator[1];
        if (parent.getKnownNodes().get(originatorID) == null) {
            parent.getKnownNodes().put(originatorID, new NodeRecord(originatorHostPort, originatorID,
                    requestOriginator[0],new Socket(requestOriginator[0], split.getPort(originatorHostPort))));
            fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
            parent.setFingerTableModified(true);
        }
    }

    public synchronized void processPayload(FilePayload filePayload, ConcurrentHashMap<Integer, File> filesResponsibleFor)
            throws IOException {
        if (filesResponsibleFor.get(filePayload.getFileID()) == null) {
            System.out.println("storing file");
            filePayload.writeFile(filePayload.getFileByteArray(), storageDirectory + filePayload.getFileName());
            synchronized (filesResponsibleFor) {
                filesResponsibleFor.put(filePayload.getFileID(), new File(storageDirectory + filePayload.getFileName()));
            }
            System.out.println("stored file");
        } else {
            Collision collision = new Collision();
            String senderHost = filePayload.getSendingNodeHostPort().split(":")[0];
            int senderPort = Integer.parseInt(filePayload.getSendingNodeHostPort().split(":")[1]);
            Socket fileOwnerSocket = new Socket(senderHost, senderPort);
            sender.sendToSpecificSocket(fileOwnerSocket, collision.getBytes());
        }
    }

    public synchronized void updateSuccessor(QueryResponse queryResponse) throws IOException {
        String newSuccessorHostPort = split.getHostPort(queryResponse.getPredecessorInfo());
        Socket newSuccessorSocket = new Socket(split.getHost(newSuccessorHostPort), split.getPort(newSuccessorHostPort));
        NodeRecord updatedSuccessor =
                new NodeRecord(newSuccessorHostPort,
                        split.getID(queryResponse.getPredecessorInfo()),
                        split.getHost(queryResponse.getPredecessorInfo()), newSuccessorSocket);
        parent.getKnownNodes().put(split.getID(queryResponse.getPredecessorInfo()), updatedSuccessor);
        fingerTableManagement.updateConcurrentFingerTable(ID, parent.getFingerTable(), parent.getKnownNodes());
        parent.setFingerTableModified(true);
        //don't necessarily have to do below unless we need to update overlay super fast
//        Query queryNewSuccessor = new Query(); //check if this node is successor to new node
//        sender.sendToSpecificSocket(updatedSuccessor.getNodeSocket(), queryNewSuccessor.getBytes());
    }

    private synchronized void sendDeadNodeMessage(String deadNodeInfo, String targetNode) throws IOException {
        DeadNode deadNode = new DeadNode();
        deadNode.setDeadNode(deadNodeInfo);
        deadNode.setOrigin(self.toString());
        Socket targetSocket = new Socket(split.getHost(targetNode), split.getPort(targetNode));
        sender.sendToSpecificSocket(targetSocket, deadNode.getBytes());
    }
}
