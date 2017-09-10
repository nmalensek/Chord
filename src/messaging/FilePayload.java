package messaging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilePayload implements Protocol, Event {
    private int messageType = FILE;
    private int fileID;
    private File payload;
    private Path filepath;
    private byte[] fileByteArray;

    @Override
    public FilePayload getType() { return this; }

    @Override
    public int getMessageType() { return messageType; }

    public File getPayload() { return payload; }
    public void setPayload(File payload) { this.payload = payload; }

    public Path getFilepath() { return filepath; }
    public void setFilepath(Path filepath) { this.filepath = filepath; }

    public int getFileID() { return fileID; }
    public void setFileID(int fileID) { this.fileID = fileID; }

    //marshalls bytes
    public byte[] getBytes() throws IOException {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream =
                new DataOutputStream(new BufferedOutputStream(byteArrayOutputStream));

        dataOutputStream.writeInt(messageType);
        dataOutputStream.writeInt(fileID);

        byte[] fileBytes = Files.readAllBytes(payload.toPath());
        int fileBytesLength = fileBytes.length;
        dataOutputStream.writeInt(fileBytesLength);
        dataOutputStream.write(fileBytes);

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
        fileID = dataInputStream.readInt();

        int fileLength = dataInputStream.readInt();
        byte[] fileBytes = new byte[fileLength];
        dataInputStream.readFully(fileBytes);

        fileByteArray = fileBytes;
//        Files.write(filepath, fileBytes);
//
//        payload = filepath.toFile();

        byteArrayInputStream.close();
        dataInputStream.close();
    }

    public void writeFile(byte[] bytes, String filepath) throws IOException {
        Path path = Paths.get(filepath);
        Files.write(path, bytes);

//        payload = filepath.toFile();
    }

}
