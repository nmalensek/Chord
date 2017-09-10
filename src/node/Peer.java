package node;

import messaging.*;
import transport.TCPSender;
import transport.TCPServerThread;
import util.ShutdownHook;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

public class Peer implements Node {

    private static int nodeIdentifier;
    private static String peerHost;
    private static int peerPort;
    private static String discoveryNodeHost;
    private static int discoveryNodePort;
    private TCPSender sender = new TCPSender();
    private NodeRecord predecessor;
    private HashMap<Integer, NodeRecord> fingerTable = new HashMap<>();

    public Peer() throws IOException {
        startup();
        connectToNetwork();
    }

    private void startup() {
        TCPServerThread serverThread = new TCPServerThread(this, peerPort);
        serverThread.start();
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
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(sender, fingerTable.get(1),
                predecessor, nodeIdentifier, discoveryNodeHost, discoveryNodePort));
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
            ((FilePayload) event).writeFile(((FilePayload) event).getFileByteArray(), "test.png");
        }
    }

    private void storeFile() {

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
        } else {

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

    @Override
    public void processText(String text) {

    }

    public static void main(String[] args) throws UnknownHostException {
        peerHost = Inet4Address.getLocalHost().getHostName();
        peerPort = Integer.parseInt(args[0]);
        discoveryNodeHost = args[1];
        discoveryNodePort = Integer.parseInt(args[2]);
        nodeIdentifier = Integer.parseInt(args[3]);

        try {
            Peer peer = new Peer();
            peer.startup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
