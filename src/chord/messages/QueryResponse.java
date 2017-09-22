package chord.messages;

import java.io.*;

public class QueryResponse implements Protocol, Event {
    private int messageType = QUERY_RESPONSE;
    private String predecessorInfo;

    public QueryResponse getType() {
        return this;
    }

    public String getPredecessorInfo() {
        return predecessorInfo;
    }

    public void setPredecessorInfo(String predecessorInfo) {
        this.predecessorInfo = predecessorInfo;
    }

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

        byte[] infoBytes = predecessorInfo.getBytes();
        int infoLength = infoBytes.length;
        dataOutputStream.writeInt(infoLength);
        dataOutputStream.write(infoBytes);

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


        int infoLength = dataInputStream.readInt();
        byte[] infoBytes = new byte[infoLength];
        dataInputStream.readFully(infoBytes);

        predecessorInfo = new String(infoBytes);

        byteArrayInputStream.close();
        dataInputStream.close();
    }
}
