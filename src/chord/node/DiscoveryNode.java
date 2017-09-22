package chord.node;

import chord.messages.*;
import chord.transport.TCPSender;
import chord.transport.TCPServerThread;
import chord.util.SplitHostPort;
import chord.utilitythreads.TextInputThread;

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
    private static String storeDataHost;
    private static int storeDataPort;
    private HashMap<Integer, NodeRecord> registeredPeers = new HashMap<>();
    private ArrayList<Integer> randomNodes = new ArrayList<>();
    private TCPSender sender = new TCPSender();
    private SplitHostPort split = new SplitHostPort();

    public DiscoveryNode() {
    }

    private void startup() {
        TCPServerThread serverThread = new TCPServerThread(this, discoveryPort);
        serverThread.start();
        TextInputThread textInputThread = new TextInputThread(this);
        textInputThread.start();
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
            System.out.println("sent node information to remote "  + node.getNodeSocket().getRemoteSocketAddress());
        } else {
            Collision collision = new Collision();
            sender.sendToSpecificSocket(node.getNodeSocket(), collision.getBytes());
        }
    }

    private synchronized NodeInformation prepareRandomNodeInfoMessage(NodeRecord randomNode) {
        NodeInformation randomNodeInfo = new NodeInformation();
        randomNodeInfo.setNodeInfo(randomNode.toString());
        return randomNodeInfo;
    }

    private synchronized void respondToInquiry() throws IOException {
        NodeRecord randomNode = chooseRandomNode();
        NodeInformation nodeToContact = prepareRandomNodeInfoMessage(randomNode);
        Socket storeDataSocket = new Socket(storeDataHost, storeDataPort);
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
            respondToInquiry();
        }
    }

    private synchronized void handleNodeLeaving(int nodeID) throws IOException {
        NodeRecord removedNode = registeredPeers.remove(nodeID);
        for (int i = 0; i < randomNodes.size(); i++) {
            if (randomNodes.get(i) == nodeID) {
                randomNodes.remove(i);
            }
        }
        System.out.println(removedNode.getNickname() +
                " (ID: " + removedNode.getIdentifier() + ") has left the overlay.");
        removedNode = null;
    }

    private synchronized NodeRecord constructNewNode(Event event, Socket newSocket) throws IOException {
        String hostPort = split.getHostPort(((NodeInformation) event).getNodeInfo());
        int identifier = split.getID(((NodeInformation) event).getNodeInfo());
        String nickname = split.getHost(((NodeInformation) event).getNodeInfo());
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
            case "d":
                if (registeredPeers.size() == 0) {
                    System.out.println("No nodes in overlay.");
                } else {
                    for (NodeRecord peer : registeredPeers.values()) {
                        System.out.println("ID: " + peer.getIdentifier() + "\tHost: " + peer.getHost() +
                                "\tPort: " + peer.getPort());
                    }
                }
                break;
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        discoveryHost = Inet4Address.getLocalHost().getHostName();
        discoveryPort = Integer.parseInt(args[0]);
        storeDataHost = args[1];
        storeDataPort = Integer.parseInt(args[2]);

        DiscoveryNode discoveryNode = new DiscoveryNode();
        discoveryNode.startup();
    }
}
