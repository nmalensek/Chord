package chord.messages;

import java.io.*;

public class DestinationNode implements Protocol, Event {

    private int messageType = DESTINATION;
    private String destinationNode;
    private String destinationPredecessor;
    private String origin;

    public DestinationNode getType() {
        return this;
    }

    public String getDestinationNode() { return destinationNode; }
    public void setDestinationNode(String destinationNode) { this.destinationNode = destinationNode; }

    public String getDestinationPredecessor() { return destinationPredecessor; }
    public void setDestinationPredecessor(String destinationPredecessor) { this.destinationPredecessor = destinationPredecessor; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    @Override
    public int getMessageType() {
        return messageType;
    }

    //marshalls bytes
    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dataOutputStream.writeInt(messageType);

        byte[] destinationBytes = destinationNode.getBytes();
        int destinationLength = destinationBytes.length;
        dataOutputStream.writeInt(destinationLength);
        dataOutputStream.write(destinationBytes);

        byte[] destinationPredecessorBytes = destinationPredecessor.getBytes();
        int destinationPredecessorLength = destinationPredecessorBytes.length;
        dataOutputStream.writeInt(destinationPredecessorLength);
        dataOutputStream.write(destinationPredecessorBytes);

        byte[] originBytes = origin.getBytes();
        int originLength = originBytes.length;
        dataOutputStream.writeInt(originLength);
        dataOutputStream.write(originBytes);

        dataOutputStream.flush();
        marshalledBytes = byteArrayOutputStream.toByteArray();

        byteArrayOutputStream.close();
        dataOutputStream.close();

        return marshalledBytes;
    }

    public void readMessage(byte[] marshalledBytes) throws IOException {
        ByteArrayInputStream byteArrayInputStream =
                new ByteArrayInputStream(marshalledBytes);
        DataInputStream dataInputStream =
                new DataInputStream(new BufferedInputStream(byteArrayInputStream));

        messageType = dataInputStream.readInt();

        int destinationLength = dataInputStream.readInt();
        byte[] destinationBytes = new byte[destinationLength];
        dataInputStream.readFully(destinationBytes);

        destinationNode = new String(destinationBytes);

        int destinationPredecessorLength = dataInputStream.readInt();
        byte[] destinationPredecessorInfo = new byte[destinationPredecessorLength];
        dataInputStream.readFully(destinationPredecessorInfo);

        destinationPredecessor = new String(destinationPredecessorInfo);

        int originBytesLength = dataInputStream.readInt();
        byte[] originNodeBytes = new byte[originBytesLength];
        dataInputStream.readFully(originNodeBytes);

        origin = new String(originNodeBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }

}
