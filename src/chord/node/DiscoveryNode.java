package chord.node;

import chord.messages.*;
import chord.transport.TCPSender;
import chord.transport.TCPServerThread;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
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
        if (registeredPeers.get(node.getIdentifier()) == null) {
            NodeRecord randomNode = randomNodes.size() == 0 ? node : chooseRandomNode();

            NodeInformation messageWithRandomNode = prepareRandomNodeInfoMessage(randomNode);
            sender.sendToSpecificSocket(node.getNodeSocket(), messageWithRandomNode.getBytes());
        } else {
            Collision collision = new Collision();
            sender.sendToSpecificSocket(node.getNodeSocket(), collision.getBytes());
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
            NodeRecord justRegisteredNode = constructNewNode(event, destinationSocket);
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

    private synchronized NodeRecord constructNewNode(Event event, Socket newSocket) throws IOException {
        String hostPort = ((NodeInformation) event).getHostPort();
        int identifier = ((NodeInformation) event).getSixteenBitID();
        String nickname = ((NodeInformation) event).getNickname();
        NodeRecord newNode = new NodeRecord(hostPort, identifier, nickname, newSocket);

        return newNode;
    }

    /**
     * Chooses random node from overlay to inform new node about.
     */
    private synchronized NodeRecord chooseRandomNode() {
        int randomNode = ThreadLocalRandom.current().nextInt(0, randomNodes.size());
        return registeredPeers.get(randomNodes.get(randomNode));
    }

    @Override
    public void processText(String text) throws IOException {
        switch (text) {
            case "diagnostic":
                for (NodeRecord peer : registeredPeers.values()) {
                    System.out.println("ID: " + peer.getIdentifier() + "Host: " + peer.getHost() +
                            "\tPort: " + peer.getPort());
                }
                break;
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        discoveryHost = Inet4Address.getLocalHost().getHostName();
        discoveryPort = Integer.parseInt(args[0]);

        DiscoveryNode discoveryNode = new DiscoveryNode();
        discoveryNode.startup();
    }
}
