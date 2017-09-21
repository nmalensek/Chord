package chord.messages;

import java.io.*;

public class Query implements Protocol, Event {
    private int messageType = QUERY;
    private String senderInfo;

    public Query getType() {
        return this;
    }

    public String getSenderInfo() { return senderInfo; }
    public void setSenderInfo(String senderInfo) { this.senderInfo = senderInfo; }

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

        byte[] senderInfoBytes = senderInfo.getBytes();
        int senderInfoLength = senderInfoBytes.length;
        dataOutputStream.writeInt(senderInfoLength);
        dataOutputStream.write(senderInfoBytes);

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

        int senderInfoLength = dataInputStream.readInt();
        byte[] senderInfoBytes = new byte[senderInfoLength];
        dataInputStream.readFully(senderInfoBytes);

        senderInfo = new String(senderInfoBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }
}
