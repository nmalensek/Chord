package node;

import data.ConvertHex;
import hash.ComputeHash;
import messaging.Collision;
import messaging.Event;
import messaging.NodeInformation;
import transport.TCPSender;
import transport.TCPServerThread;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class Peer implements Node {

    private static String initialIdentifier;
    private static String peerHost;
    private static int peerPort;
    private static String discoveryNodeHost;
    private static int discoveryNodePort;
    private String convertedIdentifier;
    private ComputeHash computeHash = new ComputeHash();
    private ConvertHex convertHex = new ConvertHex();
    private TCPSender sender = new TCPSender();
    private String predecessor;
    private HashMap<Integer, String> fingerTable = new HashMap<>();

    public Peer() throws IOException {
        convertedIdentifier = convertHex.convertBytesToHex(initialIdentifier.getBytes());
        startup();
        connectToNetwork();
    }

    private void startup() {
        TCPServerThread serverThread = new TCPServerThread(this, peerPort);
        serverThread.start();
    }

    private void connectToNetwork() throws IOException {
        NodeInformation nodeInformation = new NodeInformation();
        nodeInformation.setSixteenBitID(convertedIdentifier);
        nodeInformation.setHostPort(peerHost + ":" + peerPort);
        nodeInformation.setNickname(peerHost + ":" + peerPort);
        sender.sendToSpecificSocket(new Socket(discoveryNodeHost, discoveryNodePort), nodeInformation.getBytes());
    }

    private void constructFingerTable() {

    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        if (event instanceof NodeInformation) {
            //figure out where to go!
        } else if (event instanceof Collision) {
            //another node already has that id, get a new id and retry
            convertedIdentifier = String.valueOf(System.currentTimeMillis());
            connectToNetwork();
//        } else if (event instanceof ) {
//
//        } else if (event instanceof ) {
//
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        if (args.length == 4) {
            initialIdentifier = args[3];
        } else {
            initialIdentifier = String.valueOf(System.currentTimeMillis());
        }
        peerHost = Inet4Address.getLocalHost().getHostName();
        peerPort = Integer.parseInt(args[0]);
        discoveryNodeHost = args[1];
        discoveryNodePort = Integer.parseInt(args[2]);

        try {
            Peer peer = new Peer();
            peer.startup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
