package chord.messages;

import java.io.*;

public class AskForSuccessor implements Protocol, Event {
    private int messageType = ASK_FOR_SUCCESSOR;
    private String originatorInformation;

    public AskForSuccessor getType() {
        return this;
    }

    public String getOriginatorInformation() { return originatorInformation; }
    public void setOriginatorInformation(String originatorInformation) { this.originatorInformation = originatorInformation; }

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

        byte[] originatorBytes = originatorInformation.getBytes();
        int originatorLength = originatorBytes.length;
        dataOutputStream.writeInt(originatorLength);
        dataOutputStream.write(originatorBytes);

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

        int originatorLength = dataInputStream.readInt();
        byte[] originatorBytes = new byte[originatorLength];
        dataInputStream.readFully(originatorBytes);

        originatorInformation = new String(originatorBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }
}
