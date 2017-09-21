package chord.node;

import chord.messages.*;
import chord.messages.messageprocessors.HandleNodeLeaving;
import chord.messages.messageprocessors.MessageProcessor;
import chord.transport.TCPReceiverThread;
import chord.transport.TCPSender;
import chord.transport.TCPServerThread;
import chord.util.CreateIdentifier;
import chord.utilitythreads.DiagnosticPrinterThread;
import chord.utilitythreads.QuerySuccessorThread;
import chord.util.ShutdownHook;
import chord.utilitythreads.TextInputThread;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Peer implements Node {

    private static int nodeIdentifier;
    private static String peerHost;
    private static int peerPort;
    private static String discoveryNodeHost;
    private static int discoveryNodePort;
    private static int queryInterval;
    private static int diagnosticInterval;
    private TCPSender sender = new TCPSender();
    private TCPServerThread serverThread;
    private NodeRecord predecessor;
    private MessageProcessor messageProcessor;
    private HandleNodeLeaving handleNodeLeaving;
    private Socket discoveryNodeSocket = new Socket(discoveryNodeHost, discoveryNodePort);
    private final HashMap<Integer, NodeRecord> fingerTable = new HashMap<>();
    private final HashMap<Integer, String> filesResponsibleFor = new HashMap<>();
    private final HashMap<Integer, NodeRecord> knownNodes = new HashMap<>();
    private AtomicBoolean fingerTableModified = new AtomicBoolean();
    private AtomicBoolean filesResponsibleForModified = new AtomicBoolean();

    public Peer() throws IOException {
        startServer();
        constructInitialFingerTable();
        startUtilityThreads();
        predecessor = new NodeRecord(peerHost + ":" + peerPort, nodeIdentifier, peerHost, null);
        messageProcessor = new MessageProcessor(peerHost, peerPort, nodeIdentifier, this, null);
        handleNodeLeaving = new HandleNodeLeaving(peerHost, peerPort, nodeIdentifier, this);
        connectToNetwork();
    }

    private void startServer() {
        serverThread = new TCPServerThread(this, 0);
        System.out.println("ID:\t" + nodeIdentifier);
        serverThread.start();
        setPort();
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

    private void startUtilityThreads() throws IOException {
        TCPReceiverThread discoveryNodeReceiver = new TCPReceiverThread(discoveryNodeSocket, this);
        discoveryNodeReceiver.start();
        DiagnosticPrinterThread diagnosticPrinterThread =
                new DiagnosticPrinterThread(this, diagnosticInterval);
        diagnosticPrinterThread.start();
        QuerySuccessorThread querySuccessorThread =
                new QuerySuccessorThread(this, queryInterval, peerHost, peerPort);
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

    private synchronized void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(sender, this,
                nodeIdentifier, discoveryNodeSocket));
    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        try {
            if (event instanceof NodeInformation) {
                NodeInformation information = (NodeInformation) event;
                messageProcessor.processRegistration(information);
                addShutDownHook();
                System.out.println("Added shutdown hook");
            } else if (event instanceof Collision) {
                System.out.println("This node's ID already exists in the overlay. Please enter a new one:");
                promptForNewID();
                connectToNetwork();
            } else if (event instanceof DestinationNode) {
                messageProcessor.processDestination((DestinationNode) event);
            } else if (event instanceof Lookup) {
                messageProcessor.processLookup(((Lookup) event), predecessor);
            } else if (event instanceof FilePayload) {
//                messageProcessor.processPayload((FilePayload) event, filesResponsibleFor);
            } else if (event instanceof Query) {
                System.out.println("got a query");
                if (predecessor.getNodeSocket() != null) {
                    sender.sendToSpecificSocket(predecessor.getNodeSocket(), writeQueryResponse().getBytes()); //send predecessor info
                    System.out.println("sent a response");
                }
            } else if (event instanceof QueryResponse) {
                System.out.println("entered on QueryResponse block");
                QueryResponse queryResponseMessage = (QueryResponse) event;
                System.out.println("got a response containing: " + queryResponseMessage.getPredecessorHostPort() + ":" +
                        queryResponseMessage.getPredecessorID() + " so that means me = predecessor is " + (queryResponseMessage.getPredecessorID() == nodeIdentifier));
                if (queryResponseMessage.getPredecessorID() != nodeIdentifier) {
                    System.out.println("predecessor wasn't me :(");
                    updateSuccessor(queryResponseMessage, destinationSocket);
                }
            } else if (event instanceof UpdatePredecessor) {
                System.out.println("updating predecessor...");
                messageProcessor.processPredecessorUpdate((UpdatePredecessor) event, filesResponsibleFor);
//            } else if (event instanceof NodeLeaving) {
//                if (fingerTable.get(1).getIdentifier() == (((NodeLeaving) event).getSixteenBitID())) { //successor left
//                    handleNodeLeaving.processSuccessorLeaving((NodeLeaving) event, destinationSocket);
//                } else {
//                    handleNodeLeaving.processPredecessorLeaving((NodeLeaving) event, destinationSocket);
//                }
//            } else if (event instanceof SuccessorInformation) {
//                System.out.println("got successor information");
//                messageProcessor.processSuccessorInformation((SuccessorInformation) event);
//            } else if (event instanceof AskForSuccessor) {
//                System.out.println("got request for successor information");
//                messageProcessor.sendSuccessorInformation(fingerTable.get(1), destinationSocket);
//                messageProcessor.checkIfUnknownNode((AskForSuccessor) event);
            }
        } catch (IOException e) {
            e.printStackTrace();
//            handleNodeLeaving.removeDeadNodeAndUpdateFT(destinationSocket);
        }
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

    private synchronized void updateSuccessor(QueryResponse queryResponse, Socket successorSocket) throws IOException {
        NodeRecord updatedSuccessor =
                new NodeRecord(queryResponse.getPredecessorHostPort(),
                        queryResponse.getPredecessorID(),
                        queryResponse.getPredecessorNickname(), successorSocket);
        fingerTable.put(1, updatedSuccessor);
        fingerTableModified.set(true);
        Query queryNewSuccessor = new Query(); //check if this node is successor to new node
        sender.sendToSpecificSocket(updatedSuccessor.getNodeSocket(), queryNewSuccessor.getBytes());
    }

    public void sendFilesToSuccessor() throws IOException {
        for (int fileKey : filesResponsibleFor.keySet()) {
            FilePayload file = new FilePayload();
            file.setFileID(fileKey);
            file.setFileName(filesResponsibleFor.get(fileKey).substring(5, filesResponsibleFor.get(fileKey).length()));
            file.setFileToTransfer(new File(filesResponsibleFor.get(fileKey)));
            System.out.println("Sending file " + fileKey + " to " + fingerTable.get(1).getNickname() + "\tID: "
                    + fingerTable.get(1).getIdentifier());
            TCPSender sender = new TCPSender();
            sender.sendToSpecificSocket(fingerTable.get(1).getNodeSocket(), file.getBytes());
        }
    }

    protected void promptForNewID() {
        Scanner userInput = new Scanner(System.in);
        try {
            nodeIdentifier = Integer.parseInt(userInput.nextLine());
        } catch (NumberFormatException n) {
            System.out.println("Identifier must be an integer between 0 and 65535, please try again.");
            promptForNewID();
        }
    }

    public HashMap<Integer, NodeRecord> getFingerTable() {
        synchronized (fingerTable) {
            return fingerTable;
        }
    }

    public HashMap<Integer, String> getFilesResponsibleFor() {
        synchronized (filesResponsibleFor) {
            return filesResponsibleFor;
        }
    }

    public NodeRecord getPredecessor() {
        synchronized (predecessor) {
            return predecessor;
        }
    }

    public void setPredecessor(NodeRecord newPredecessor) {
        synchronized (predecessor) {
            this.predecessor = newPredecessor;
        }
    }

    public HashMap<Integer, NodeRecord> getKnownNodes() {
        synchronized (knownNodes) {
            return knownNodes;
        }
    }

    public boolean isFingerTableModified() {
        return fingerTableModified.get();
    }

    public boolean isFilesResponsibleForModified() {
        return filesResponsibleForModified.get();
    }

    public void setFingerTableModified(boolean newValue) {
        fingerTableModified.set(newValue);
    }

    public void setFilesResponsibleForModified(boolean newValue) {
        filesResponsibleForModified.set(newValue);
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

    public static void main(String[] args) throws UnknownHostException {
        peerHost = Inet4Address.getLocalHost().getHostName();
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
            Peer peer = new Peer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
