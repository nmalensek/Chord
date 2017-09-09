package messaging;

import java.io.*;

public class Lookup implements Protocol, Event {
    private int messageType = LOOKUP;
    private String pathIDs;
    private String payloadID;
    private int numHops;

    @Override
    public Lookup getType() { return this; }

    @Override
    public int getMessageType() { return messageType; }

    public String getPathIDs() { return pathIDs; }
    public void setPathIDs(String pathIDs) { this.pathIDs = pathIDs; }

    public String getPayloadID() { return payloadID; }
    public void setPayloadID(String payloadID) { this.payloadID = payloadID; }

    public int getNumHops() { return numHops; }
    public void setNumHops(int numHops) { this.numHops = numHops; }

    //marshalls bytes
    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dataOutputStream.writeInt(messageType);
        dataOutputStream.writeInt(numHops);

        byte[] pathIDsBytes = pathIDs.getBytes();
        int identifierLength = pathIDsBytes.length;
        dataOutputStream.writeInt(identifierLength);
        dataOutputStream.write(pathIDsBytes);

        byte[] payloadBytes = payloadID.getBytes();
        int payloadLength = payloadBytes.length;
        dataOutputStream.writeInt(payloadLength);
        dataOutputStream.write(payloadBytes);

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
        numHops = dataInputStream.readInt();

        int pathIDLength = dataInputStream.readInt();
        byte[] pathIDBytes = new byte[pathIDLength];
        dataInputStream.readFully(pathIDBytes);

        pathIDs = new String(pathIDBytes);

        int payloadLength = dataInputStream.readInt();
        byte[] payloadBytes = new byte[payloadLength];
        dataInputStream.readFully(payloadBytes);

        payloadID = new String(payloadBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }
}
