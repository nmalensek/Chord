package chord.test;

import chord.messages.*;
import chord.node.Node;
import chord.node.NodeRecord;
import chord.transport.TCPReceiverThread;
import chord.transport.TCPSender;
import chord.transport.TCPServerThread;
import chord.util.CreateIdentifier;
import chord.util.FingerTableManagement;
import chord.util.SplitHostPort;
import chord.utilitythreads.TextInputThread;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueryTest implements Node {

    private static int nodeIdentifier;
    private static String peerHost;
    private static int peerPort;
    private static String discoveryNodeHost;
    private static int discoveryNodePort;
    private static int queryInterval;
    private static int diagnosticInterval;
    private TCPServerThread serverThread;
    private SplitHostPort split = new SplitHostPort();
    private NodeRecord self;
    private NodeRecord predecessor;
    private Socket discoveryNodeSocket = new Socket(discoveryNodeHost, discoveryNodePort);
    private final ConcurrentHashMap<Integer, NodeRecord> fingerTable = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> filesResponsibleFor = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, NodeRecord> knownNodes = new ConcurrentHashMap<>();
    private AtomicBoolean fingerTableModified = new AtomicBoolean();
    private FingerTableManagement fingerTableManagement = new FingerTableManagement();
    private TCPSender sender = new TCPSender();

    public QueryTest() throws IOException {
        startServer();
        constructInitialFingerTable();
        startUtilityThreads();
        self = new NodeRecord(peerHost + ":" + peerPort, nodeIdentifier, peerHost, null);
        predecessor = new NodeRecord(peerHost + ":" + peerPort, nodeIdentifier, peerHost, null);
        knownNodes.put(nodeIdentifier, self);
        setPredecessor(self);
        connectToNetwork();
    }

    public synchronized void processDestination(DestinationNode destinationNode) throws IOException {
        //a destination means that node's your successor, so set successor
        String successorInfo = destinationNode.getDestinationNode();
        Socket successorSocket =
                new Socket(split.getHost(successorInfo), split.getPort(successorInfo));

        NodeRecord successorNode = new NodeRecord(
                split.getHostPort(successorInfo), split.getID(successorInfo), split.getHost(successorInfo), successorSocket);

        getFingerTable().put(1, successorNode);
        getKnownNodes().put(split.getID(successorInfo), successorNode);

        String predecessorInfo = destinationNode.getDestinationPredecessor();
        if (getKnownNodes().get(split.getID(predecessorInfo)) == null) {
            Socket predecessorSocket = new Socket(split.getHost(predecessorInfo), split.getPort(predecessorInfo));
            String predHost = split.getHost(predecessorInfo);
            int predPort = split.getPort(predecessorInfo);
            int predID = split.getID(predecessorInfo);
            NodeRecord newPredecessor = new NodeRecord(predHost + ":" + predPort, predID, predHost, predecessorSocket);
            getKnownNodes().put(predID, newPredecessor);
            setPredecessor(newPredecessor);
        } else {
            setPredecessor(getKnownNodes().get(split.getID(predecessorInfo)));
        }

        UpdatePredecessor updatePredecessor = new UpdatePredecessor(); //tell successor to update its predecessor
        updatePredecessor.setPredecessorID(nodeIdentifier);
        updatePredecessor.setPredecessorHostPort(peerHost + ":" + peerPort);
        updatePredecessor.setPredecessorNickname(peerHost);
        sender.sendToSpecificSocket(successorNode.getNodeSocket(), updatePredecessor.getBytes());

        fingerTableManagement.updateConcurrentFingerTable(nodeIdentifier, getFingerTable(), getKnownNodes());
        fingerTableManagement.printConcurrentFingerTable(getFingerTable());
    }

    public synchronized void processRegistration(NodeInformation information) throws IOException {
        if (information.getSixteenBitID() != nodeIdentifier) { //not first node in the ring so find out where to go
            System.out.println("message ID: " + information.getSixteenBitID() + " vs. my ID: " + nodeIdentifier);
//            getKnownNodes().remove(nodeIdentifier); //remove self from known nodes so FT is ok
            Lookup findLocation = new Lookup();
            findLocation.setRoutingPath(self.toString() + ",");
            findLocation.setPayloadID(nodeIdentifier);
            Socket randomNodeSocket = new Socket(split.getHost(information.getHostPort()),
                    split.getPort(information.getHostPort()));
            sender.sendToSpecificSocket(randomNodeSocket, findLocation.getBytes());
            getKnownNodes().put(information.getSixteenBitID(),
                    new NodeRecord(information.getHostPort(),
                            information.getSixteenBitID(),
                            information.getNickname(), randomNodeSocket));
        }
    }


    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        if (event instanceof NodeInformation) {
            NodeInformation information = (NodeInformation) event;
            processRegistration(information);
        } else if (event instanceof DestinationNode) {
            processDestination((DestinationNode) event);
        } else if (event instanceof Lookup) {
            processLookup(((Lookup) event), predecessor);
//        } else if (event instanceof Query) {
//            System.out.println("got a query");
//            sender.sendToSpecificSocket(destinationSocket, writeQueryResponse().getBytes()); //send predecessor info
//            System.out.println("sent a response");
//        } else if (event instanceof QueryResponse) {
//            System.out.println("entered QueryResponse block");
//            QueryResponse queryResponseMessage = (QueryResponse) event;
//            System.out.println("got a response containing: " + queryResponseMessage.getPredecessorHostPort() + ":" +
//                    queryResponseMessage.getPredecessorID() + " so that means me = predecessor is " + (queryResponseMessage.getPredecessorID() == nodeIdentifier));
//            if (queryResponseMessage.getPredecessorID() != nodeIdentifier) {
//                System.out.println("predecessor wasn't me :(");
//            }
        } else if (event instanceof UpdatePredecessor) {
            System.out.println("updating predecessor...");
            processPredecessorUpdate((UpdatePredecessor) event, filesResponsibleFor, destinationSocket);
        } else if (event instanceof TestMessage) {
//            System.out.println("got a test");
            TestResponse testResponse = new TestResponse();
            System.out.println("predecessor: " + predecessor.toString());
            if (predecessor.getNodeSocket() != null) {
                sender.sendToSpecificSocket(predecessor.getNodeSocket(), testResponse.getBytes());
            }
//            System.out.println("sent a test response");
        } else if (event instanceof TestResponse) {
            System.out.println("GOT A TEST RESPONSE GREAT SUCCESS");
        }
    }

    public synchronized void processLookup(Lookup lookupEvent, NodeRecord predecessor) throws IOException {
        int payload = lookupEvent.getPayloadID();
        int predecessorID = predecessor.getIdentifier();
        NodeRecord successor = getFingerTable().get(1);
        sendDestinationMessage(lookupEvent, self.toString(), predecessor.toString());
//        if (payload > predecessorID && payload <= nodeIdentifier) { //normal message storage condition
//            sendDestinationMessage(lookupEvent, self.toString(), predecessor.toString());
//        } else if (predecessorID == nodeIdentifier) { //second node joins overlay
//            sendDestinationMessage(lookupEvent, self.toString(), self.toString());
//        } else if (predecessorID == successor.getIdentifier()) { //third node joins overlay
//            NodeRecord larger = nodeIdentifier > predecessorID ? self : predecessor;
//            NodeRecord smaller = nodeIdentifier < predecessorID ? self : predecessor;
//            if (payload > larger.getIdentifier() || payload < smaller.getIdentifier()) {
//                sendDestinationMessage(lookupEvent, smaller.toString(), larger.toString());
//            } else if (smaller.getIdentifier() < payload && payload < larger.getIdentifier()) {
//                sendDestinationMessage(lookupEvent, larger.toString(), smaller.toString());
//            }
//        } else if (successor.getIdentifier() > payload && predecessorID < payload) { //Successor is largest, send to there
//            forwardLookup(lookupEvent, successor);
//        } else {
//            ConcurrentHashMap<Integer, NodeRecord> parentFingerTable = getFingerTable();
//            for (int key : parentFingerTable.keySet()) {
//                NodeRecord currentRow = parentFingerTable.get(key);
//                NodeRecord nextRow = parentFingerTable.get(key + 1);
//                if (nextRow == null) {
//                    forwardLookup(lookupEvent, currentRow); //you're at the last FT entry, forward to currentRow
//                    break;
//                } else if (currentRow.getIdentifier() <= payload && payload < nextRow.getIdentifier()) {
//                    forwardLookup(lookupEvent, currentRow); //currentRow is largest FT row that's still less than k, send to there
//                    break;
//                }
//            }
//        }
//        getKnownNodes().remove(nodeIdentifier); //know you're not the only one in the ring anymore, remove yourself
    }

    private synchronized void sendDestinationMessage(Lookup lookup, String successorHostPortID, String predecessorHostPortID) throws IOException {
        DestinationNode thisNodeIsSink = new DestinationNode();
        thisNodeIsSink.setDestinationNode(successorHostPortID);
        thisNodeIsSink.setDestinationPredecessor(predecessorHostPortID);
        System.out.println("sent destination message with predecessor information of "
                + thisNodeIsSink.getDestinationPredecessor() + "\nand successor information: "
                + ":" + thisNodeIsSink.getDestinationNode());

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
        if (getKnownNodes().get(originatingID) == null) {
            socket = new Socket(originatingHost, originatingPort);
            NodeRecord node = new NodeRecord(originatingHost + ":" + originatingPort, originatingID, originatingHost, socket);
            getKnownNodes().put(originatingID, node);
            fingerTableManagement.updateConcurrentFingerTable(nodeIdentifier, getFingerTable(), getKnownNodes());
        } else {
            socket = getKnownNodes().get(originatingID).getNodeSocket();
        }
        return socket;
    }

    private synchronized void forwardLookup(Lookup lookup, NodeRecord forwardTarget) throws IOException {
        lookup.setRoutingPath(lookup.getRoutingPath() + self.toString() + ",");
        lookup.setNumHops((lookup.getNumHops() + 1));
        Socket successorSocket = forwardTarget.getNodeSocket();
        sender.sendToSpecificSocket(successorSocket, lookup.getBytes());
        System.out.println("Hops: " + lookup.getNumHops() + "\tfor id: " + lookup.getPayloadID());
    }

    private synchronized QueryResponse writeQueryResponse() throws IOException {
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setPredecessorID(predecessor.getIdentifier());
        queryResponse.setPredecessorHostPort(predecessor.getHost() + ":" + predecessor.getPort());
        queryResponse.setPredecessorNickname(predecessor.getNickname());
        System.out.println(queryResponse.getPredecessorID() + "\t" + queryResponse.getPredecessorHostPort() + "\t"
                + queryResponse.getMessageType());
        return queryResponse;
    }

    private void setPort() {
        while (true) {
            try {
                peerPort = serverThread.getPortNumber();
                if (peerPort != 0) {
                    break;
                }
            } catch (NullPointerException npe) {

            }
        }
    }

    private void startServer() {
        serverThread = new TCPServerThread(this, 0);
        System.out.println("ID:\t" + nodeIdentifier);
        serverThread.start();
        setPort();
    }

    @Override
    public void processText(String text) {
        switch (text) {
            case "d":
                System.out.println("Finger table at this node:");
                for (int key : fingerTable.keySet()) {
                    NodeRecord currentRow = fingerTable.get(key);
                    System.out.println(key + "\t" + currentRow.getHost() + ":" + currentRow.getPort()
                            + "\tID: " + currentRow.getIdentifier());
                }
                System.out.println("\nSuccessor and predecessor:");
                System.out.println("Successor: " + fingerTable.get(1).getHost() + "(ID: " + fingerTable.get(1).getIdentifier() + ")");
                System.out.println("Predecessor: " + predecessor.getHost() + "(ID: " + predecessor.getIdentifier() + ")");
                System.out.println("\nFiles managed by this node:");
                for (int key : filesResponsibleFor.keySet()) {
                    System.out.println(key + "\t" + filesResponsibleFor.get(key));
                }
                System.out.println("Known nodes: ");
                for (int node : knownNodes.keySet()) {
                    System.out.printf(knownNodes.get(node) + "\t");
                }
                System.out.println("");
                break;
            default:
                System.out.println("Valid commands are: d for diagnostics");
        }
    }

    private void setPredecessor(NodeRecord newPredecessor) {
        synchronized (predecessor) {
            this.predecessor = newPredecessor;
        }
    }

    private void startUtilityThreads() throws IOException {
        TCPReceiverThread discoveryNodeReceiver = new TCPReceiverThread(discoveryNodeSocket, this);
        discoveryNodeReceiver.start();
        TestSuccessorThread querySuccessorThread =
                new TestSuccessorThread(this, queryInterval, peerHost, peerPort);
        querySuccessorThread.start();
        TextInputThread textInputThread = new TextInputThread(this);
        textInputThread.start();
    }

    private void connectToNetwork() throws IOException {
        NodeInformation nodeInformation = new NodeInformation();
        nodeInformation.setSixteenBitID(nodeIdentifier);
        nodeInformation.setHostPort(peerHost + ":" + peerPort);
        nodeInformation.setNickname(peerHost + ":" + peerPort);
        sender.sendToSpecificSocket(discoveryNodeSocket, nodeInformation.getBytes());
    }

    private void constructInitialFingerTable() throws IOException {
        NodeRecord thisNode = new NodeRecord(peerHost + ":" + peerPort,
                nodeIdentifier, Inet4Address.getLocalHost().getHostName(), null);
        for (int i = 1; i < 17; i++) { //16-bit ID space, so all nodes have 16 FT entries.
            fingerTable.put(i, thisNode);
        }
        fingerTableModified.set(true);
    }


    public ConcurrentHashMap<Integer, NodeRecord> getFingerTable() {
        synchronized (fingerTable) {
            return fingerTable;
        }
    }

    public ConcurrentHashMap<Integer, String> getFilesResponsibleFor() {
        synchronized (filesResponsibleFor) {
            return filesResponsibleFor;
        }
    }

    public NodeRecord getPredecessor() {
        synchronized (predecessor) {
            return predecessor;
        }
    }

    public ConcurrentHashMap<Integer, NodeRecord> getKnownNodes() {
        synchronized (knownNodes) {
            return knownNodes;
        }
    }

    public synchronized void processPredecessorUpdate(UpdatePredecessor updatePredecessor, ConcurrentHashMap<Integer, String> fileHashMap,
                                                      Socket predecessorSocket) throws IOException {
        String newPredecessorHostPort = updatePredecessor.getPredecessorHostPort();
        int newPredecessorID = updatePredecessor.getPredecessorID();
        String newPredecessorNickname = updatePredecessor.getPredecessorNickname();
        Socket testSocket = new Socket(split.getHost(newPredecessorHostPort), split.getPort(newPredecessorHostPort));
        NodeRecord newPredecessor =
                new NodeRecord(newPredecessorHostPort, newPredecessorID, newPredecessorNickname, testSocket);
        setPredecessor(newPredecessor);
        getKnownNodes().putIfAbsent(newPredecessorID, newPredecessor);

        fingerTableManagement.updateConcurrentFingerTable(nodeIdentifier, getFingerTable(), getKnownNodes());

//        if (parent.getKnownNodes().size() != 1) {
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

    public static void main(String[] args) {
        try {
            peerHost = Inet4Address.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        queryInterval = Integer.parseInt(args[1]);
        diagnosticInterval = Integer.parseInt(args[2]);
        discoveryNodeHost = args[3];
        discoveryNodePort = Integer.parseInt(args[4]);

        if (args[0].equals("na")) {
            try {
                nodeIdentifier = CreateIdentifier.createIdentifier(String.valueOf(System.currentTimeMillis()));

            } catch (StringIndexOutOfBoundsException e) {
                nodeIdentifier = CreateIdentifier.createIdentifier(String.valueOf(System.currentTimeMillis()));
            }
        } else {
            nodeIdentifier = Integer.parseInt(args[0]);
        }

        try {
            QueryTest queryTest = new QueryTest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
