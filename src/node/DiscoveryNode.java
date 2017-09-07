package node;

import messaging.Collision;
import messaging.Event;
import messaging.NodeInformation;
import messaging.NodeLeaving;
import transport.TCPSender;
import transport.TCPServerThread;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DiscoveryNode implements Node {

    private static String discoveryHost;
    private static int discoveryPort;
    private HashMap<String, NodeRecord> registeredPeers = new HashMap<>();
    private ArrayList<NodeRecord> randomNodes = new ArrayList<>();
    private TCPSender sender = new TCPSender();

    public DiscoveryNode() {
        TCPServerThread serverThread = new TCPServerThread(this, discoveryPort);
        serverThread.start();
    }

    private synchronized void addNewNodeToOverlay(NodeRecord newNode) throws IOException {
            if (randomNodes.size() == 0) {
                checkForCollision(newNode);
            } else {
                registeredPeers.put(newNode.getIdentifier(), newNode);
                randomNodes.add(newNode);
            }
    }

    private synchronized void checkForCollision(NodeRecord node) throws IOException {
        Socket newNodeSocket = new Socket(node.getHost(), node.getPort());
        try {
            if (registeredPeers.get(node.getIdentifier()) == null) {
                NodeRecord randomNode = chooseRandomNode(); //choose from overlay nodes before new node is added.
                registeredPeers.put(node.getIdentifier(), node);
                randomNodes.add(node);

                NodeInformation messageWithRandomNode = prepareAcceptanceMessage(randomNode);
                sender.sendToSpecificSocket(newNodeSocket, messageWithRandomNode.getBytes());
            } else {
                Collision collision = new Collision();
                sender.sendToSpecificSocket(newNodeSocket, collision.getBytes());
            }
        } finally {
            newNodeSocket.close();
        }
    }

    private NodeInformation prepareAcceptanceMessage(NodeRecord randomNode) {
        NodeInformation randomNodeInfo = new NodeInformation();
        randomNodeInfo.setSixteenBitID(randomNode.getIdentifier());
        randomNodeInfo.setHostPort(randomNode.getHost() + ":" + randomNode.getPort());
        randomNodeInfo.setNickname(randomNode.getNickname());
        return randomNodeInfo;
    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        if (event instanceof NodeInformation) {
            NodeRecord justRegisteredNode = constructNewNode(event);
            addNewNodeToOverlay(justRegisteredNode);
        } else if (event instanceof NodeLeaving) {
            handleNodeLeaving(((NodeLeaving) event).getSixteenBitID());
        }
    }

    private synchronized void handleNodeLeaving(String nodeID) {
        NodeRecord removedNode = registeredPeers.remove(nodeID);
        randomNodes.remove(removedNode);
    }

    private NodeRecord constructNewNode(Event event) throws IOException {
        String hostPort = ((NodeInformation) event).getHostPort();
        String identifier = ((NodeInformation) event).getSixteenBitID();
        String nickname = ((NodeInformation) event).getNickname();
        NodeRecord node = new NodeRecord(hostPort, identifier, nickname);

        return node;
    }

    /**
     * Chooses random node from overlay to inform new node about.
     */
    private synchronized NodeRecord chooseRandomNode() {
        int randomNode = ThreadLocalRandom.current().nextInt(0, randomNodes.size());
        return registeredPeers.get(randomNodes.get(randomNode));
    }

    public static void main(String[] args) {
        discoveryHost = args[0];
        discoveryPort = Integer.parseInt(args[1]);

        DiscoveryNode discoveryNode = new DiscoveryNode();
    }
}
