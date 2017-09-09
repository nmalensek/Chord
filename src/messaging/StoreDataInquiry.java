package messaging;

import java.io.*;

public class StoreDataInquiry implements Protocol, Event{
    private int messageType = STORE_DATA_INQUIRY;
    private String sixteenBitID;

    public StoreDataInquiry getType() {
        return this;
    }

    public String getSixteenBitID() { return sixteenBitID; }
    public void setSixteenBitID(String sixteenBitID) { this.sixteenBitID = sixteenBitID; }

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

        byteArrayInputStream.close();
        dataInputStream.close();
    }
}
