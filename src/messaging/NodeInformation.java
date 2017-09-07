package messaging;

import java.io.*;

public class NodeInformation implements Protocol, Event {
    private int messageType = ENTER_OVERLAY;
    private String sixteenBitID;
    private String hostPort;
    private String nickname;

    public NodeInformation getType() {
        return this;
    }

    public String getSixteenBitID() { return sixteenBitID; }
    public void setSixteenBitID(String sixteenBitID) { this.sixteenBitID = sixteenBitID; }

    public String getHostPort() { return hostPort; }
    public void setHostPort(String hostPort) { this.hostPort = hostPort; }{}

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

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

        byte[] identifierBytes = sixteenBitID.getBytes();
        int identifierLength = identifierBytes.length;
        dataOutputStream.writeInt(identifierLength);
        dataOutputStream.write(identifierBytes);

        byte[] hostPortBytes = hostPort.getBytes();
        int hostPortLength = hostPortBytes.length;
        dataOutputStream.writeInt(hostPortLength);
        dataOutputStream.write(hostPortBytes);

        byte[] nicknameBytes = nickname.getBytes();
        int nicknameLength = nicknameBytes.length;
        dataOutputStream.writeInt(nicknameLength);
        dataOutputStream.write(nicknameBytes);

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

        int identifierLength = dataInputStream.readInt();
        byte[] identifierBytes = new byte[identifierLength];
        dataInputStream.readFully(identifierBytes);

        sixteenBitID = new String(identifierBytes);

        int hostPortLength = dataInputStream.readInt();
        byte[] hostPortBytes = new byte[hostPortLength];
        dataInputStream.readFully(hostPortBytes);

        hostPort = new String(hostPortBytes);

        int nicknameLength = dataInputStream.readInt();
        byte[] nicknameBytes = new byte[nicknameLength];
        dataInputStream.readFully(nicknameBytes);

        nickname = new String(nicknameBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }



}
