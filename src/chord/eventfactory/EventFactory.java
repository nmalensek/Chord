package chord.eventfactory;

import chord.messages.*;
import chord.test.TestMessage;
import chord.test.TestResponse;

import java.io.IOException;

public final class EventFactory {

    /**
     * Class creates events based on the type of message received at a chord.messaging.node so the chord.messaging.node can respond accordingly.
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

    public static Event<Query> queryEvent(
            byte[] marshalledBytes) throws IOException {
        Query query = new Query();
        query.readMessage(marshalledBytes);
        return query;
    }

    public static Event<QueryResponse> queryResponseEvent(
            byte[] marshalledBytes) throws IOException {
        QueryResponse response = new QueryResponse();
        response.readMessage(marshalledBytes);
        return response;
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

    public static Event<StoreDataInquiry> storeDataInquiryEvent(
            byte[] marshalledBytes) throws IOException {
        StoreDataInquiry inquiry = new StoreDataInquiry();
        inquiry.readMessage(marshalledBytes);
        return  inquiry;
    }

    public static Event<Lookup> lookupEvent(
            byte[] marshalledBytes) throws IOException {
        Lookup lookup = new Lookup();
        lookup.readMessage(marshalledBytes);
        return lookup;
    }

    public static Event<DestinationNode> destinationNodeEvent(
            byte[] marshalledBytes) throws IOException {
        DestinationNode destinationNode = new DestinationNode();
        destinationNode.readMessage(marshalledBytes);
        return destinationNode;
    }

    public static Event<UpdatePredecessor> updatePredecessorEvent(
            byte[] marshalledBytes) throws IOException {
        UpdatePredecessor updatePredecessor = new UpdatePredecessor();
        updatePredecessor.readMessage(marshalledBytes);
        return updatePredecessor;
    }

    public static Event<AskForSuccessor> askForSuccessorEvent(
            byte[] marshalledBytes) throws IOException {
        AskForSuccessor askForSuccessor = new AskForSuccessor();
        askForSuccessor.readMessage(marshalledBytes);
        return askForSuccessor;
    }

    public static Event<SuccessorInformation> successorInformationEvent(
            byte[] marshalledBytes) throws IOException {
        SuccessorInformation successorInformation = new SuccessorInformation();
        successorInformation.readMessage(marshalledBytes);
        return successorInformation;
    }

    public static Event<FilePayload> filePayloadEvent(
            byte[] marshalledBytes) throws IOException {
        FilePayload filePayload = new FilePayload();
        filePayload.readMessage(marshalledBytes);
        return filePayload;
    }

    public static Event<DeadNode> deadNodeEvent(
            byte[] marshalledBytes) throws IOException {
        DeadNode deadNode = new DeadNode();
        deadNode.readMessage(marshalledBytes);
        return deadNode;
    }

    public static Event<TestMessage> testMessageEvent(
            byte[] marshalledBytes) throws IOException {
        TestMessage testMessage = new TestMessage();
        testMessage.readMessage(marshalledBytes);
        return testMessage;
    }

    public static Event<TestResponse> testResponseEvent(
            byte[] marshalledBytes) throws IOException {
     TestResponse testResponse = new TestResponse();
     testResponse.readMessage(marshalledBytes);
     return testResponse;
    }

}
