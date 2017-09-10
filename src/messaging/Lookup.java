package messaging;

import java.io.*;

public class Lookup implements Protocol, Event {
    private int messageType = LOOKUP;
    private int payloadID;
    private int numHops;
    private String routingPath;

    @Override
    public Lookup getType() { return this; }

    @Override
    public int getMessageType() { return messageType; }

    public int getPayloadID() { return payloadID; }
    public void setPayloadID(int payloadID) { this.payloadID = payloadID; }

    public int getNumHops() { return numHops; }
    public void setNumHops(int numHops) { this.numHops = numHops; }

    public String getRoutingPath() { return routingPath; }
    public void setRoutingPath(String routingPath) { this.routingPath = routingPath; }

    //marshalls bytes
    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dataOutputStream.writeInt(messageType);
        dataOutputStream.writeInt(payloadID);
        dataOutputStream.writeInt(numHops);

        byte[] routingPathBytes = routingPath.getBytes();
        int routingPathLength = routingPathBytes.length;
        dataOutputStream.writeInt(routingPathLength);
        dataOutputStream.write(routingPathBytes);

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
        payloadID = dataInputStream.readInt();
        numHops = dataInputStream.readInt();

        int routingPathLength = dataInputStream.readInt();
        byte[] routingPathBytes = new byte[routingPathLength];
        dataInputStream.readFully(routingPathBytes);

        routingPath = new String(routingPathBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }
}
