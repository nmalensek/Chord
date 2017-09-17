package chord.messages;

import java.io.*;

public class SuccessorInformation implements Protocol, Event {
    private int messageType = SUCCESSOR_INFO;
    private int successorID;
    private String successorHostPort;
    private String successorNickname;

    public SuccessorInformation getType() { return this; }

    public int getSuccessorID() { return successorID; }
    public void setSuccessorID(int successorID) { this.successorID = successorID; }

    public String getSuccessorHostPort() { return successorHostPort; }
    public void setSuccessorHostPort(String successorHostPort) { this.successorHostPort = successorHostPort; }

    public String getSuccessorNickname() { return successorNickname; }
    public void setSuccessorNickname(String successorNickname) { this.successorNickname = successorNickname; }

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

        dataOutputStream.writeInt(successorID);

        byte[] hostPortBytes = successorHostPort.getBytes();
        int hostPortLength = hostPortBytes.length;
        dataOutputStream.writeInt(hostPortLength);
        dataOutputStream.write(hostPortBytes);

        byte[] nicknameBytes = successorNickname.getBytes();
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

        successorID = dataInputStream.readInt();

        int hostPortLength = dataInputStream.readInt();
        byte[] hostPortBytes = new byte[hostPortLength];
        dataInputStream.readFully(hostPortBytes);

        successorHostPort = new String(hostPortBytes);

        int nicknameLength = dataInputStream.readInt();
        byte[] nicknameBytes = new byte[nicknameLength];
        dataInputStream.readFully(nicknameBytes);

        successorNickname = new String(nicknameBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }
}
