package chord.messages;

import java.io.*;

public class DestinationNode implements Protocol, Event {

    private int messageType = DESTINATION;
    private int destinationID;
    private String hostPort;
    private String destinationNickname;
    private String destinationPredecessor;

    public DestinationNode getType() {
        return this;
    }

    public String getHostPort() { return hostPort; }
    public void setHostPort(String hostPort) { this.hostPort = hostPort; }

    public int getDestinationID() { return destinationID; }
    public void setDestinationID(int destinationID) { this.destinationID = destinationID; }

    public String getDestinationNickname() { return destinationNickname; }
    public void setDestinationNickname(String destinationNickname) { this.destinationNickname = destinationNickname; }

    public String getDestinationPredecessor() { return destinationPredecessor; }
    public void setDestinationPredecessor(String destinationPredecessor) { this.destinationPredecessor = destinationPredecessor; }

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
        dataOutputStream.writeInt(destinationID);

        byte[] hostPortBytes = hostPort.getBytes();
        int hostPortLength = hostPortBytes.length;
        dataOutputStream.writeInt(hostPortLength);
        dataOutputStream.write(hostPortBytes);

        byte[] destNickname = destinationNickname.getBytes();
        int nickLength = destNickname.length;
        dataOutputStream.writeInt(nickLength);
        dataOutputStream.write(destNickname);

        byte[] destinationPredecessorBytes = destinationPredecessor.getBytes();
        int destinationPredecessorLength = destinationPredecessorBytes.length;
        dataOutputStream.writeInt(destinationPredecessorLength);
        dataOutputStream.write(destinationPredecessorBytes);

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
        destinationID = dataInputStream.readInt();

        int hostPortLength = dataInputStream.readInt();
        byte[] hostPortBytes = new byte[hostPortLength];
        dataInputStream.readFully(hostPortBytes);

        hostPort = new String(hostPortBytes);

        int nicknameLength = dataInputStream.readInt();
        byte[] nicknameBytes = new byte[nicknameLength];
        dataInputStream.readFully(nicknameBytes);

        destinationNickname = new String(nicknameBytes);

        int destinationPredecessorLength = dataInputStream.readInt();
        byte[] destinationPredecessorInfo = new byte[destinationPredecessorLength];
        dataInputStream.readFully(destinationPredecessorInfo);

        destinationPredecessor = new String(destinationPredecessorInfo);

        byteArrayInputStream.close();
        dataInputStream.close();
    }

}
