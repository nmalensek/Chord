package chord.messages;

import java.io.*;

public class DestinationNode implements Protocol, Event {

    private int messageType = DESTINATION;
    private String hostPort;

    public DestinationNode getType() {
        return this;
    }

    public String getHostPort() { return hostPort; }
    public void setHostPort(String hostPort) { this.hostPort = hostPort; }{}

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

        byte[] hostPortBytes = hostPort.getBytes();
        int hostPortLength = hostPortBytes.length;
        dataOutputStream.writeInt(hostPortLength);
        dataOutputStream.write(hostPortBytes);

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

        int hostPortLength = dataInputStream.readInt();
        byte[] hostPortBytes = new byte[hostPortLength];
        dataInputStream.readFully(hostPortBytes);

        hostPort = new String(hostPortBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }

}
