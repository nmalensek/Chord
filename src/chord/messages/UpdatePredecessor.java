package chord.messages;

import java.io.*;

public class UpdatePredecessor implements Protocol, Event {
        private int messageType = UPDATE;
        private String predecessorInfo;

        public UpdatePredecessor getType() { return this; }

    public String getPredecessorInfo() { return predecessorInfo; }
    public void setPredecessorInfo(String predecessorInfo) { this.predecessorInfo = predecessorInfo; }

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

            byte[] predecessorBytes = predecessorInfo.getBytes();
            int predecessorLength = predecessorBytes.length;
            dataOutputStream.writeInt(predecessorLength);
            dataOutputStream.write(predecessorBytes);

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

            int predecessorLength = dataInputStream.readInt();
            byte[] predecessorBytes = new byte[predecessorLength];
            dataInputStream.readFully(predecessorBytes);

            predecessorInfo = new String(predecessorBytes);

            byteArrayInputStream.close();
            dataInputStream.close();
        }
    }