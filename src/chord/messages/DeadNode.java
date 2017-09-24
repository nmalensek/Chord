package chord.messages;

import java.io.*;

public class DeadNode implements Protocol, Event {
    private int messageType = DEAD_NODE;
    private String deadNode;
    private String origin;

    public DeadNode getType() {
        return this;
    }

    public String getDeadNode() { return deadNode; }
    public void setDeadNode(String deadNode) { this.deadNode = deadNode; }

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

        byte[] deadNodeBytes = deadNode.getBytes();
        int deadNodeLength = deadNodeBytes.length;
        dataOutputStream.writeInt(deadNodeLength);
        dataOutputStream.write(deadNodeBytes);

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

        int deadNodeLength = dataInputStream.readInt();
        byte[] deadNodeBytes = new byte[deadNodeLength];
        dataInputStream.readFully(deadNodeBytes);

        deadNode = new String(deadNodeBytes);

        int originBytesLength = dataInputStream.readInt();
        byte[] originNodeBytes = new byte[originBytesLength];
        dataInputStream.readFully(originNodeBytes);

        origin = new String(originNodeBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }
}
