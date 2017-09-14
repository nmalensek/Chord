package chord.node;

import chord.messages.*;
import chord.transport.TCPSender;
import chord.transport.TCPServerThread;
import chord.util.CreateIdentifier;
import chord.util.DiagnosticPrinterThread;
import chord.util.QuerySuccessorThread;
import chord.util.ShutdownHook;

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
    private final HashMap<Integer, NodeRecord> fingerTable = new HashMap<>();
    private final HashMap<Integer, String> filesResponsibleFor = new HashMap<>();
    private AtomicBoolean fingerTableModified = new AtomicBoolean();
    private AtomicBoolean filesResponsibleForModified = new AtomicBoolean();

    public Peer() throws IOException {
        startThreads();
        setPort();
        connectToNetwork();
    }

    private void startThreads() {
        serverThread = new TCPServerThread(this, 0);
        serverThread.start();
        DiagnosticPrinterThread diagnosticPrinterThread =
                new DiagnosticPrinterThread(this, diagnosticInterval);
        diagnosticPrinterThread.start();
        QuerySuccessorThread querySuccessorThread =
                new QuerySuccessorThread(this, queryInterval);
        querySuccessorThread.start();
    }

    private void setPort() {
        while(serverThread.getPortNumber() == 0) {
            peerPort = serverThread.getPortNumber();
        }
    }

    private void connectToNetwork() throws IOException {
        NodeInformation nodeInformation = new NodeInformation();
        nodeInformation.setSixteenBitID(nodeIdentifier);
        nodeInformation.setHostPort(peerHost + ":" + peerPort);
        nodeInformation.setNickname(peerHost + ":" + peerPort);
        sender.sendToSpecificSocket(new Socket(discoveryNodeHost, discoveryNodePort), nodeInformation.getBytes());
    }

    private void constructFingerTable() {
        for (int i = 1; i < 17; i++) { //16-bit ID space, so all nodes have 16 FT entries.

        }
    }

    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(sender, this,
                nodeIdentifier, discoveryNodeHost, discoveryNodePort));
    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        if (event instanceof NodeInformation) {
            //figure out where to go!
            addShutDownHook();
        } else if (event instanceof Collision) {
            //another node already has that id, get a new id and retry
            System.out.println("This node's ID already exists in the overlay. Please enter a new one:");
            promptForNewID();
            connectToNetwork();
        } else if (event instanceof NodeLeaving) {
            if (fingerTable.get(1).getIdentifier() == (((NodeLeaving) event).getSixteenBitID())) {
                //successor left
                fingerTable.remove(1); //TODO: re-make finger table instead of just remove
            } else {
                //predecessor left
                predecessor = null;
            }
            //update finger table
        } else if (event instanceof Lookup) {
            processLookup(((Lookup) event));
        } else if (event instanceof FilePayload) {
            FilePayload file = (FilePayload) event;
            file.writeFile(file.getFileByteArray(), "/tmp/" + file.getFileName());
            synchronized (filesResponsibleFor) {
                filesResponsibleFor.put(file.getFileID(), "/tmp/" + file.getFileName());
            }
        } else if (event instanceof Query) {
            //send predecessor info
            sender.sendToSpecificSocket(destinationSocket, writeQueryResponse());
        } else if (event instanceof QueryResponse) {
            QueryResponse queryResponseMessage = (QueryResponse) event;
            if (queryResponseMessage.getPredecessorID() != nodeIdentifier) {
                updateSuccessor(queryResponseMessage);
            }
        }
    }

    private byte[] writeQueryResponse() throws IOException {
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setPredecessorID(predecessor.getIdentifier());
        queryResponse.setPredecessorHostPort(predecessor.getHost() + ":" + predecessor.getPort());
        queryResponse.setPredecessorNickname(predecessor.getNickname());
        return queryResponse.getBytes();
    }

    private void updateSuccessor(QueryResponse queryResponse) throws IOException {
        NodeRecord updatedSuccessor =
                new NodeRecord(queryResponse.getPredecessorHostPort(),
                        queryResponse.getPredecessorID(),
                        queryResponse.getPredecessorNickname());
        fingerTable.put(0, updatedSuccessor);
        fingerTableModified.set(true);
        Query queryNewSuccessor = new Query(); //check if this node is successor to new node
        sender.sendToSpecificSocket(
                new Socket(fingerTable.get(0).getHost(), fingerTable.get(0).getPort()), queryNewSuccessor.getBytes());
    }

    private void processLookup(Lookup lookupEvent) throws IOException {
        int payload = lookupEvent.getPayloadID();
        if (payload > predecessor.getIdentifier() && payload <= nodeIdentifier) {
            DestinationNode thisNodeIsSink = new DestinationNode();
            thisNodeIsSink.setHostPort(peerHost + ":" + peerPort);
            String originatingNode = lookupEvent.getRoutingPath().split(",")[0];
            String originatingHost = originatingNode.split(":")[0];
            int originatingPort = Integer.parseInt(originatingNode.split(":")[1]);
            sender.sendToSpecificSocket(new Socket(originatingHost, originatingPort), thisNodeIsSink.getBytes());
            System.out.println((lookupEvent.getNumHops() + 1));
        } else {
            //route message to appropriate node
            lookupEvent.setNumHops((lookupEvent.getNumHops() + 1));
            System.out.println(lookupEvent.getNumHops());
            //TODO route message using finger table
        }
    }

    private void promptForNewID() {
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

    public boolean isFingerTableModified() { return fingerTableModified.get(); }
    public boolean isFilesResponsibleForModified() { return filesResponsibleForModified.get(); }

    @Override
    public void processText(String text) { }

    public static void main(String[] args) throws UnknownHostException {
        peerHost = Inet4Address.getLocalHost().getHostName();
        queryInterval = Integer.parseInt(args[1]);
        diagnosticInterval = Integer.parseInt(args[2]);
        discoveryNodeHost = args[3];
        discoveryNodePort = Integer.parseInt(args[4]);

        if (args[0].equals("na")) {
            nodeIdentifier = CreateIdentifier.createIdentifier(String.valueOf(System.currentTimeMillis()));
        } else {
            nodeIdentifier = Integer.parseInt(args[0]);
        }

        try {
            Peer peer = new Peer();
            peer.startThreads();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
