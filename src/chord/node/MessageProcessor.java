package chord.node;

import chord.messages.*;
import chord.transport.TCPSender;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

class MessageProcessor {

    private TCPSender sender = new TCPSender();
    private String host;
    private int port;
    private int ID;
    private Peer parent;
    private NodeRecord self;

    MessageProcessor(String host, int port, int ID, Peer parent) throws IOException {
        this.host = host;
        this.port = port;
        this.ID = ID;
        this.parent = parent;
        self = new NodeRecord(host + ":" + port, ID, host + ":" + port);
    }

    void processRegistration(NodeInformation information) throws IOException {
        if (information.getSixteenBitID() != ID) { //not first node in the ring so find out where to go
            Lookup findLocation = new Lookup();
            findLocation.setRoutingPath(host + ":" + port + ",");
            findLocation.setPayloadID(ID);
            Socket randomNodeSocket = new Socket(information.getHostPort().split(":")[0],
                    Integer.parseInt(information.getHostPort().split(":")[1]));
            sender.sendToSpecificSocket(randomNodeSocket, findLocation.getBytes());
        } else {
            parent.setPredecessor(self);
        }
    }

    void processLookup(Lookup lookupEvent, NodeRecord predecessor) throws IOException {
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
                    //you're at the last FT entry, forward to currentRow
                    forwardLookup(lookupEvent, currentRow);
                    break;
                } else if (currentRow.getIdentifier() <= payload && payload < nextRow.getIdentifier()) {
                    //currentRow is largest FT row that's still less than k, send to there
                    forwardLookup(lookupEvent, currentRow);
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
        lookup.setRoutingPath(lookup.getRoutingPath() + "," + host + ":" + port);
        lookup.setNumHops((lookup.getNumHops() + 1));
        Socket successorSocket = new Socket(forwardTarget.getHost(), forwardTarget.getPort());
        sender.sendToSpecificSocket(successorSocket, lookup.getBytes());
        System.out.println("Hops: " + lookup.getNumHops() + "\tfor id: " + lookup.getPayloadID());
    }

    void processDestination(DestinationNode destinationNode) throws IOException {
        //a destination means that node's your successor, so set successor and send a message to update its predecessor
        String successorHostPort = destinationNode.getHostPort();
        int successorID = destinationNode.getDestinationID();
        String successorNickname = destinationNode.getDestinationNickname();
        NodeRecord successorNode = new NodeRecord(successorHostPort, successorID, successorNickname);
        parent.getFingerTable().put(1, successorNode);

        UpdatePredecessor updatePredecessor = new UpdatePredecessor();
        updatePredecessor.setPredecessorID(ID);
        updatePredecessor.setPredecessorHostPort(host + ":" + port);
        updatePredecessor.setPredecessorNickname(host);
        sender.sendToSpecificSocket(successorNode.getNodeSocket(), updatePredecessor.getBytes());
    }

    void processPredecessorUpdate(UpdatePredecessor updatePredecessor) throws IOException {
        String predecessorHostPort = updatePredecessor.getPredecessorHostPort();
        int predecessorID = updatePredecessor.getPredecessorID();
        String predecessorNickname = updatePredecessor.getPredecessorNickname();
        NodeRecord newPredecessor = new NodeRecord(predecessorHostPort, predecessorID, predecessorNickname);
        parent.setPredecessor(newPredecessor);
        //TODO transfer data items as necessary!
    }

    void processPayload(FilePayload filePayload, HashMap<Integer, String> filesResponsibleFor) throws IOException {
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
            fileOwnerSocket.close();
        }
    }
}
