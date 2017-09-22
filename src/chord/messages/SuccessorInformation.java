package chord.messages;

import java.io.*;

public class SuccessorInformation implements Protocol, Event {
    private int messageType = SUCCESSOR_INFO;
    private String successorInfo;

    public SuccessorInformation getType() { return this; }

    public String getSuccessorInfo() { return successorInfo; }
    public void setSuccessorInfo(String successorInfo) { this.successorInfo = successorInfo; }

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

        byte[] successorBytes = successorInfo.getBytes();
        int successorLength = successorBytes.length;
        dataOutputStream.writeInt(successorLength);
        dataOutputStream.write(successorBytes);

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

        int successorLength = dataInputStream.readInt();
        byte[] successorBytes = new byte[successorLength];
        dataInputStream.readFully(successorBytes);

        successorInfo = new String(successorBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }
}
