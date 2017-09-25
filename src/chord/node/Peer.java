package chord.node;

import chord.messages.*;
import chord.messages.messageprocessors.HandleNodeLeaving;
import chord.messages.messageprocessors.MessageProcessor;
import chord.transport.TCPReceiverThread;
import chord.transport.TCPSender;
import chord.transport.TCPServerThread;
import chord.util.CreateIdentifier;
import chord.util.SplitHostPort;
import chord.utilitythreads.DiagnosticPrinterThread;
import chord.utilitythreads.QuerySuccessorThread;
import chord.util.ShutdownHook;
import chord.utilitythreads.TextInputThread;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Peer implements Node {

    private static int peerIdentifier;
    private static String peerHost;
    private static int peerPort;
    private static String discoveryNodeHost;
    private static int discoveryNodePort;
    private static int queryInterval;
    private static int diagnosticInterval;
    private static String storeDataHostPort;
    private TCPSender sender = new TCPSender();
    private TCPServerThread serverThread;
    private TextInputThread textInputThread = new TextInputThread(this);
    private QuerySuccessorThread querySuccessorThread;
    private NodeRecord predecessor;
    private MessageProcessor messageProcessor;
    private HandleNodeLeaving handleNodeLeaving;
    private Socket discoveryNodeSocket = new Socket(discoveryNodeHost, discoveryNodePort);
    private Socket storeDataSocket =
            new Socket(storeDataHostPort.split(":")[0], Integer.parseInt(storeDataHostPort.split(":")[1]));
    private ConcurrentHashMap<Integer, NodeRecord> fingerTable = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, File> filesResponsibleFor = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, NodeRecord> knownNodes = new ConcurrentHashMap<>();
    private AtomicBoolean fingerTableModified = new AtomicBoolean();
    private AtomicBoolean filesResponsibleForModified = new AtomicBoolean();
    private SplitHostPort split = new SplitHostPort();
//    static volatile boolean running = true;

    public Peer() throws IOException {
        startServer();
        constructInitialFingerTable();
        startUtilityThreads();
        predecessor = new NodeRecord(peerHost + ":" + peerPort, peerIdentifier, peerHost, null);
        messageProcessor = new MessageProcessor(peerHost, peerPort, peerIdentifier, this, null, storeDataSocket);
        handleNodeLeaving = new HandleNodeLeaving(peerHost, peerPort, peerIdentifier, this);
        connectToNetwork();
    }

    private void startServer() {
        serverThread = new TCPServerThread(this, 0);
        System.out.println("ID:\t" + peerIdentifier);
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
        TCPReceiverThread storeDataReceiver = new TCPReceiverThread(storeDataSocket, this);
        discoveryNodeReceiver.start();
        storeDataReceiver.start();
//        DiagnosticPrinterThread diagnosticPrinterThread =
//                new DiagnosticPrinterThread(this, diagnosticInterval);
//        diagnosticPrinterThread.start();
        querySuccessorThread =
                new QuerySuccessorThread(this, queryInterval, peerHost, peerPort, peerIdentifier);
        querySuccessorThread.start();
        textInputThread.start();
    }

    private void connectToNetwork() throws IOException {
        NodeInformation nodeInformation = new NodeInformation();
        nodeInformation.setNodeInfo(peerHost + ":" + peerPort + ":" + peerIdentifier);
        sender.sendToSpecificSocket(discoveryNodeSocket, nodeInformation.getBytes());
    }

    private void constructInitialFingerTable() throws IOException {
        NodeRecord thisNode = new NodeRecord(peerHost + ":" + peerPort,
                peerIdentifier, Inet4Address.getLocalHost().getHostName(), null);
        for (int i = 1; i < 17; i++) { //16-bit ID space, so all nodes have 16 FT entries.
            fingerTable.put(i, thisNode);
        }
        fingerTableModified.set(true);
    }

    private synchronized void addShutDownHook() {
        final Thread mainThread = Thread.currentThread();
        ShutdownHook hook = new ShutdownHook(sender, this,
                peerIdentifier, discoveryNodeSocket, mainThread);
        hook.addThreadToInterrupt(textInputThread);
        hook.addThreadToInterrupt(querySuccessorThread);
        Runtime.getRuntime().addShutdownHook(hook);

    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
            if (event instanceof NodeInformation) {
                NodeInformation information = (NodeInformation) event;
                messageProcessor.processRegistration(information);
                addShutDownHook();
//                System.out.println("Added shutdown hook");
            } else if (event instanceof Collision) {
                System.out.println("This node's ID already exists in the overlay. Please enter a new one:");
                promptForNewID();
                connectToNetwork();
            } else if (event instanceof DestinationNode) {
                messageProcessor.processDestination((DestinationNode) event);
            } else if (event instanceof Lookup) {
                messageProcessor.processLookup(((Lookup) event), predecessor);
            } else if (event instanceof FilePayload) {
                messageProcessor.processPayload((FilePayload) event, filesResponsibleFor);
            } else if (event instanceof Query) {
                if (predecessor.getNodeSocket() != null) {
                    Query query = (Query) event;
                    String senderHost = split.getHost(query.getSenderInfo());
                    int senderPort = split.getPort(query.getSenderInfo());
                    int senderID = split.getID(query.getSenderInfo());
                    if (knownNodes.get(senderID) == null) {
                        Socket s = new Socket(senderHost, senderPort);
                        NodeRecord n = new NodeRecord(senderHost + ":" + senderPort, senderID, senderHost, s);
                        knownNodes.put(senderID, n);
                    } else {
                        sender.sendToSpecificSocket(knownNodes.get(senderID).getNodeSocket(),
                                writeQueryResponse().getBytes()); //send predecessor info
                    }
                }
            } else if (event instanceof QueryResponse) {
                QueryResponse queryResponseMessage = (QueryResponse) event;
                if (split.getID(queryResponseMessage.getPredecessorInfo()) != peerIdentifier) {
                    messageProcessor.updateSuccessor(queryResponseMessage);
                }
            } else if (event instanceof UpdatePredecessor) {
                messageProcessor.processPredecessorUpdate((UpdatePredecessor) event, filesResponsibleFor);
            } else if (event instanceof NodeLeaving) {
                if (fingerTable.get(1).getIdentifier() == (((NodeLeaving) event).getSixteenBitID())) { //successor left
                    handleNodeLeaving.processSuccessorLeaving((NodeLeaving) event);
                }
                if (predecessor.getIdentifier() == ((NodeLeaving) event).getSixteenBitID()) { //predecessor left
                    handleNodeLeaving.processPredecessorLeaving((NodeLeaving) event);
                }
            } else if (event instanceof SuccessorInformation) {
                messageProcessor.processSuccessorInformation((SuccessorInformation) event);
            } else if (event instanceof AskForSuccessor) {
                messageProcessor.sendSuccessorInformation(fingerTable.get(1), (AskForSuccessor) event, null);
                messageProcessor.checkIfUnknownNode((AskForSuccessor) event);
            } else if (event instanceof DeadNode) {
                handleNodeLeaving.removeDeadNodeUpdateFTAndForward((DeadNode) event);
            }
    }

    private synchronized QueryResponse writeQueryResponse() throws IOException {
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setPredecessorInfo(predecessor.toString());
//        System.out.println(predecessor.toString());
        return queryResponse;
    }

    public synchronized void sendFilesToSuccessor() throws IOException {
        for (int fileKey : filesResponsibleFor.keySet()) {
            FilePayload file = new FilePayload();
            file.setFileID(fileKey);
            file.setFileName(filesResponsibleFor.get(fileKey).getName());
            file.setFileToTransfer(filesResponsibleFor.get(fileKey));
            file.setSendingNodeHostPort(peerHost + ":" + peerPort);
            System.out.println("Sending file " + fileKey + " to " + fingerTable.get(1).getNickname() + "\tID: "
                    + fingerTable.get(1).getIdentifier());
            TCPSender sender = new TCPSender();
            sender.sendToSpecificSocket(fingerTable.get(1).getNodeSocket(), file.getBytes());

            File transferredFile = filesResponsibleFor.remove(fileKey);
            Files.delete(Paths.get(transferredFile.getPath()));
        }
    }

    protected void promptForNewID() {
        Scanner userInput = new Scanner(System.in);
        try {
            peerIdentifier = Integer.parseInt(userInput.nextLine());
        } catch (NumberFormatException n) {
            System.out.println("Identifier must be an integer between 0 and 65535, please try again.");
            promptForNewID();
        }
    }

    public ConcurrentHashMap<Integer, NodeRecord> getFingerTable() {
        return fingerTable;
    }

    public ConcurrentHashMap<Integer, File> getFilesResponsibleFor() {
        return filesResponsibleFor;
    }

    public ConcurrentHashMap<Integer, NodeRecord> getKnownNodes() {
        return knownNodes;
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
                System.out.println("Finger table at this node (ID " + peerIdentifier + "):\n");
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
            case "s":
                for (int node : knownNodes.keySet()) {
                    System.out.print(knownNodes.get(node).toString() + " ");
                    if (knownNodes.get(node).getIdentifier() != peerIdentifier) {
                        System.out.println(knownNodes.get(node).getNodeSocket().isClosed());
                    }
                }
                break;
            default:
                System.out.println("Valid commands are: d for diagnostics");
        }
    }

    public static void main(String[] args) throws UnknownHostException {

        if (args[0].equals("na")) {
            try {
                peerIdentifier = CreateIdentifier.createIdentifier(String.valueOf(System.currentTimeMillis()));

            } catch (StringIndexOutOfBoundsException e) {
                peerIdentifier = CreateIdentifier.createIdentifier(String.valueOf(System.currentTimeMillis()));
            }
        } else {
            peerIdentifier = Integer.parseInt(args[0]);
        }

        peerHost = Inet4Address.getLocalHost().getHostName();
        queryInterval = Integer.parseInt(args[1]);
        diagnosticInterval = Integer.parseInt(args[2]);
        discoveryNodeHost = args[3];
        discoveryNodePort = Integer.parseInt(args[4]);
        storeDataHostPort = args[5];

        try {
            Peer peer = new Peer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
