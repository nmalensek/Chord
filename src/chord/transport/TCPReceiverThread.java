package chord.transport;

import chord.eventfactory.EventFactory;
import chord.messages.*;
import chord.node.Node;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * code adapted from code provided by instructor at http://www.cs.colostate.edu/~cs455/lectures/CS455-HelpSession1.pdf
 */

public class TCPReceiverThread extends Thread implements Protocol {

    private Socket communicationSocket;
    private DataInputStream dataInputStream;
    private Node node;
    private EventFactory eventFactory = EventFactory.getInstance();

    public TCPReceiverThread(Socket communicationSocket, Node node) throws IOException {
        this.communicationSocket = communicationSocket;
        this.node = node;
        dataInputStream = new DataInputStream(communicationSocket.getInputStream());
    }

    /**
     * Listens for a message coming in.
     **/
    public void run() {
        int dataLength;
        while (communicationSocket != null) {
            try {
                dataLength = dataInputStream.readInt();

                byte[] data = new byte[dataLength];
                dataInputStream.readFully(data, 0, dataLength);

                determineMessageType(data);

            } catch (IOException ioe) {
                System.out.println("Node at " + communicationSocket.getRemoteSocketAddress() + " unavailable, closing connection.");
                communicationSocket = null;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads first line of message to determine the message type, then passes that to a switch statement to process
     * the message the rest of the way and pass it to the chord.messaging.node.
     *
     * @param marshalledBytes packaged message
     * @throws IOException
     */
    private void determineMessageType(byte[] marshalledBytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream =
                new ByteArrayInputStream(marshalledBytes);
        DataInputStream dataInputStream =
                new DataInputStream(new BufferedInputStream(byteArrayInputStream));

        int messageType = dataInputStream.readInt();
        dataInputStream.close();

        switch (messageType) {
            case ENTER_OVERLAY:
                Event<NodeInformation> nodeInformationEvent =
                        eventFactory.nodeInformationEvent(marshalledBytes);
                node.onEvent(nodeInformationEvent, communicationSocket);
                break;
            case ENTRANCE_SUCCESSFUL:
                Event<NodeInformation> successfulEntranceEvent =
                        eventFactory.successfulEntranceEvent(marshalledBytes);
                node.onEvent(successfulEntranceEvent, communicationSocket);
                break;
            case COLLISION:
                Event<Collision> collisionEvent =
                        eventFactory.collisionEvent(marshalledBytes);
                node.onEvent(collisionEvent, communicationSocket);
                break;
            case QUERY:
                Event<Query> queryEvent =
                        eventFactory.queryEvent(marshalledBytes);
                node.onEvent(queryEvent, communicationSocket);
            case QUERY_RESPONSE:
                Event<QueryResponse> queryResponseEvent =
                        eventFactory.queryResponseEvent(marshalledBytes);
                node.onEvent(queryResponseEvent, communicationSocket);
            case STORE_DATA_INQUIRY:
                Event<StoreDataInquiry> storeDataInquiryEvent =
                        eventFactory.storeDataInquiryEvent(marshalledBytes);
                node.onEvent(storeDataInquiryEvent, communicationSocket);
                break;
            case LOOKUP:
                Event<Lookup> lookupEvent =
                        eventFactory.lookupEvent(marshalledBytes);
                node.onEvent(lookupEvent, communicationSocket);
                break;
            case DESTINATION:
                Event<DestinationNode> destinationNodeEvent =
                        eventFactory.destinationNodeEvent(marshalledBytes);
                node.onEvent(destinationNodeEvent, communicationSocket);
                break;
            case UPDATE:
                Event<UpdatePredecessor> updatePredecessorEvent =
                        eventFactory.updatePredecessorEvent(marshalledBytes);
                node.onEvent(updatePredecessorEvent, communicationSocket);
                break;
            case ASK_FOR_SUCCESSOR:
                Event<AskForSuccessor> askForSuccessorEvent =
                        eventFactory.askForSuccessorEvent(marshalledBytes);
                node.onEvent(askForSuccessorEvent, communicationSocket);
                break;
            case SUCCESSOR_INFO:
                Event<SuccessorInformation> successorInformationEvent =
                        eventFactory.successorInformationEvent(marshalledBytes);
                node.onEvent(successorInformationEvent, communicationSocket);
                    break;
            case EXIT_OVERLAY:
                Event<NodeLeaving> nodeLeavingEvent =
                        eventFactory.nodeLeavingEvent(marshalledBytes);
                node.onEvent(nodeLeavingEvent, communicationSocket);
                communicationSocket.close();
                break;
            case FILE:
                Event<FilePayload> filePayloadEvent =
                        eventFactory.filePayloadEvent(marshalledBytes);
                node.onEvent(filePayloadEvent, communicationSocket);
                break;
            default:
                System.out.println("Something went horribly wrong, please restart.");
        }
    }
}
