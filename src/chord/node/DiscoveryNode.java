package chord.node;

import chord.messages.*;
import chord.transport.TCPSender;
import chord.transport.TCPServerThread;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DiscoveryNode implements Node {

    private static String discoveryHost;
    private static int discoveryPort;
    private HashMap<Integer, NodeRecord> registeredPeers = new HashMap<>();
    private ArrayList<Integer> randomNodes = new ArrayList<>();
    private TCPSender sender = new TCPSender();

    public DiscoveryNode() {
    }

    private void startup() {
        TCPServerThread serverThread = new TCPServerThread(this, discoveryPort);
        serverThread.start();
    }

    private synchronized void addNewNodeToOverlay(NodeRecord newNode) throws IOException {
        checkForCollision(newNode);
        registeredPeers.put(newNode.getIdentifier(), newNode);
        randomNodes.add(newNode.getIdentifier());
    }

    private synchronized void checkForCollision(NodeRecord node) throws IOException {
        Socket newNodeSocket = new Socket(node.getHost(), node.getPort());
        try {
            if (registeredPeers.get(node.getIdentifier()) == null) {
                NodeRecord randomNode = randomNodes.size() == 0 ? node : chooseRandomNode();

                NodeInformation messageWithRandomNode = prepareRandomNodeInfoMessage(randomNode);
                sender.sendToSpecificSocket(newNodeSocket, messageWithRandomNode.getBytes());
            } else {
                Collision collision = new Collision();
                sender.sendToSpecificSocket(newNodeSocket, collision.getBytes());
            }
        } finally {
            newNodeSocket.close();
        }
    }

    private synchronized NodeInformation prepareRandomNodeInfoMessage(NodeRecord randomNode) {
        NodeInformation randomNodeInfo = new NodeInformation();
        randomNodeInfo.setSixteenBitID(randomNode.getIdentifier());
        randomNodeInfo.setHostPort(randomNode.getHost() + ":" + randomNode.getPort());
        randomNodeInfo.setNickname(randomNode.getNickname());
        return randomNodeInfo;
    }

    private synchronized void respondToInquiry(Socket storeDataSocket) throws IOException {
            NodeRecord randomNode = chooseRandomNode();
            NodeInformation nodeToContact = prepareRandomNodeInfoMessage(randomNode);
            sender.sendToSpecificSocket(storeDataSocket, nodeToContact.getBytes());
    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        if (event instanceof NodeInformation) {
            NodeRecord justRegisteredNode = constructNewNode(event);
            addNewNodeToOverlay(justRegisteredNode);
        } else if (event instanceof NodeLeaving) {
            handleNodeLeaving(((NodeLeaving) event).getSixteenBitID());
        } else if (event instanceof StoreDataInquiry) {
            respondToInquiry(destinationSocket);
        }
    }

    private synchronized void handleNodeLeaving(int nodeID) {
        NodeRecord removedNode = registeredPeers.remove(nodeID);
        randomNodes.remove(removedNode.getIdentifier());
        System.out.println(removedNode.getNickname() +
                " (ID: " + removedNode.getIdentifier() + ") has left the overlay.");
    }

    private synchronized NodeRecord constructNewNode(Event event) throws IOException {
        String hostPort = ((NodeInformation) event).getHostPort();
        int identifier = ((NodeInformation) event).getSixteenBitID();
        String nickname = ((NodeInformation) event).getNickname();

        return new NodeRecord(hostPort, identifier, nickname);
    }

    /**
     * Chooses random node from overlay to inform new node about.
     */
    private synchronized NodeRecord chooseRandomNode() {
        int randomNode = ThreadLocalRandom.current().nextInt(0, randomNodes.size());
        return registeredPeers.get(randomNodes.get(randomNode));
    }

    @Override
    public void processText(String text) throws IOException { }

    public static void main(String[] args) {
        discoveryHost = args[0];
        discoveryPort = Integer.parseInt(args[1]);

        DiscoveryNode discoveryNode = new DiscoveryNode();
        discoveryNode.startup();
    }
}
