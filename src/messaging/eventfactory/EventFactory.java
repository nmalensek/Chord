package messaging.eventfactory;

import messaging.Collision;
import messaging.Event;
import messaging.NodeInformation;
import messaging.NodeLeaving;

import java.io.IOException;

public final class EventFactory {

    /**
     * Class creates events based on the type of message received at a node so the node can respond accordingly.
     */

    private static final EventFactory instance = new EventFactory();

    private EventFactory() { }

    public static EventFactory getInstance() {
        return instance;
    }

    public static Event<NodeInformation> nodeInformationEvent(
            byte[] marshalledBytes) throws IOException {
        NodeInformation nodeInformation = new NodeInformation();
        nodeInformation.readMessage(marshalledBytes);
        return nodeInformation;
    }

    public static Event<NodeInformation> successfulEntranceEvent(
            byte[] marshalledBytes) throws IOException {
        NodeInformation successfulEntry = new NodeInformation();
        successfulEntry.readMessage(marshalledBytes);
        return successfulEntry;
    }

    public static Event<Collision> collisionEvent(
            byte[] marshalledBytes) throws IOException {
        Collision collision = new Collision();
        collision.readMessage(marshalledBytes);
        return collision;
    }

    public static Event<NodeLeaving> nodeLeavingEvent(
            byte[] marshalledBytes) throws IOException {
        NodeLeaving nodeLeaving = new NodeLeaving();
        nodeLeaving.readMessage(marshalledBytes);
        return nodeLeaving;
    }
}
