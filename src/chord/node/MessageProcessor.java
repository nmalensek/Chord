package chord.node;

import chord.messages.Collision;
import chord.messages.DestinationNode;
import chord.messages.FilePayload;
import chord.messages.Lookup;
import chord.transport.TCPSender;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class MessageProcessor {

    TCPSender sender = new TCPSender();
    private String host;
    private int port;
    private int ID;

    public MessageProcessor(String host, int port, int ID) {
        this.host = host;
        this.port = port;
        this.ID = ID;
    }

    public void processRegistration() {

    }

    public void processLookup(Lookup lookupEvent, NodeRecord predecessor) throws IOException {
        int payload = lookupEvent.getPayloadID();
        if (payload > predecessor.getIdentifier() && payload <= ID) {
            DestinationNode thisNodeIsSink = new DestinationNode();
            thisNodeIsSink.setHostPort(host + ":" + port);
            String originatingNode = lookupEvent.getRoutingPath().split(",")[0];
            String originatingHost = originatingNode.split(":")[0];
            int originatingPort = Integer.parseInt(originatingNode.split(":")[1]);
            Socket requestorSocket = new Socket(originatingHost, originatingPort);
            sender.sendToSpecificSocket(requestorSocket, thisNodeIsSink.getBytes());
            System.out.println("Hops: " + (lookupEvent.getNumHops() + 1));
        } else {
            //route message to appropriate node
            lookupEvent.setNumHops((lookupEvent.getNumHops() + 1));
            System.out.println("Hops: " + lookupEvent.getNumHops());
            //TODO route message using finger table
        }
    }

    public void processPayload(FilePayload filePayload, HashMap<Integer, String> filesResponsibleFor) throws IOException {
        if (filesResponsibleFor.get(filePayload.getFileID()) != null) {
            filePayload.writeFile(filePayload.getFileByteArray(), "/tmp/" + filePayload.getFileName());
            synchronized (filesResponsibleFor) {
                filesResponsibleFor.put(filePayload.getFileID(), "/tmp/" + filePayload.getFileName());
            }
        } else {
            Collision collision = new Collision();
            String senderHost = filePayload.getSendingNodeHostPort().split(":")[0];
            int senderPort = Integer.parseInt(filePayload.getSendingNodeHostPort().split(":")[1]);
            Socket fileOwnerSocket = new Socket(senderHost, senderPort);
            sender.sendToSpecificSocket(fileOwnerSocket, collision.getBytes());
            fileOwnerSocket.close();
        }
    }

}
