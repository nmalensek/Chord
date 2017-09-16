package chord.messages;

import java.io.*;

public class UpdatePredecessor implements Protocol, Event {
        private int messageType = UPDATE;
        private int predecessorID;
        private String predecessorHostPort;
        private String predecessorNickname;

        public UpdatePredecessor getType() { return this; }

        public int getPredecessorID() { return predecessorID; }
        public void setPredecessorID(int predecessorID) { this.predecessorID = predecessorID; }

        public String getPredecessorHostPort() { return predecessorHostPort; }
        public void setPredecessorHostPort(String predecessorHostPort) { this.predecessorHostPort = predecessorHostPort; }

        public String getPredecessorNickname() { return predecessorNickname; }
        public void setPredecessorNickname(String predecessorNickname) { this.predecessorNickname = predecessorNickname; }

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

            dataOutputStream.writeInt(predecessorID);

            byte[] hostPortBytes = predecessorHostPort.getBytes();
            int hostPortLength = hostPortBytes.length;
            dataOutputStream.writeInt(hostPortLength);
            dataOutputStream.write(hostPortBytes);

            byte[] nicknameBytes = predecessorNickname.getBytes();
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

            predecessorID = dataInputStream.readInt();

            int hostPortLength = dataInputStream.readInt();
            byte[] hostPortBytes = new byte[hostPortLength];
            dataInputStream.readFully(hostPortBytes);

            predecessorHostPort = new String(hostPortBytes);

            int nicknameLength = dataInputStream.readInt();
            byte[] nicknameBytes = new byte[nicknameLength];
            dataInputStream.readFully(nicknameBytes);

            predecessorNickname = new String(nicknameBytes);

            byteArrayInputStream.close();
            dataInputStream.close();
        }
    }