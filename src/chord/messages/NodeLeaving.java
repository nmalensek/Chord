package chord.messages;

import java.io.*;

public class NodeLeaving implements Protocol, Event {
    private int messageType = EXIT_OVERLAY;
    private int sixteenBitID;
    private String predecessorInfo;
    private String successorInfo;

    public NodeLeaving getType() {
        return this;
    }

    public int getSixteenBitID() { return sixteenBitID; }
    public void setSixteenBitID(int sixteenBitID) { this.sixteenBitID = sixteenBitID; }

    public String getPredecessorInfo() { return predecessorInfo; }
    public void setPredecessorInfo(String predecessorInfo) { this.predecessorInfo = predecessorInfo; }

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
        dataOutputStream.writeInt(sixteenBitID);

        byte[] predecessorBytes = predecessorInfo.getBytes();
        int predecessorLength = predecessorBytes.length;
        dataOutputStream.writeInt(predecessorLength);
        dataOutputStream.write(predecessorBytes);

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

        sixteenBitID = dataInputStream.readInt();

        int predecessorLength = dataInputStream.readInt();
        byte[] predecessorBytes = new byte[predecessorLength];
        dataInputStream.readFully(predecessorBytes);

        predecessorInfo = new String(predecessorBytes);

        int successorLength = dataInputStream.readInt();
        byte[] successorBytes = new byte[successorLength];
        dataInputStream.readFully(successorBytes);

        successorInfo = new String(successorBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }

}
