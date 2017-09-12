package chord.messages;

import java.io.*;

public class StoreDataInquiry implements Protocol, Event{
    private int messageType = STORE_DATA_INQUIRY;
    private int sixteenBitID;

    public StoreDataInquiry getType() {
        return this;
    }

    public int getSixteenBitID() { return sixteenBitID; }
    public void setSixteenBitID(int sixteenBitID) { this.sixteenBitID = sixteenBitID; }

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

        byteArrayInputStream.close();
        dataInputStream.close();
    }
}
