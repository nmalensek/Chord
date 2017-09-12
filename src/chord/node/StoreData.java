package chord.node;

import chord.messages.*;
import chord.transport.TCPSender;
import chord.transport.TCPServerThread;
import chord.util.TextInputThread;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StoreData implements Node {

    private static String discoveryNodeHost;
    private static int discoveryNodePort;
    private static String thisNodeHost;
    private static int thisNodePort;
    private TCPSender sender = new TCPSender();
    private Socket discoveryNodeSocket = new Socket(discoveryNodeHost, discoveryNodePort);
    private int fileID;
    private Path filePath;
    private File file;

    public StoreData() throws IOException {
        thisNodeHost = Inet4Address.getLocalHost().getHostName();
    }

    private void startup() throws IOException {
        startServerThread();
        listenForTextInput();
    }

    private void startServerThread() {
        TCPServerThread listener = new TCPServerThread(this, 0);
        listener.start();
        while (listener.getPortNumber() == 0) {
            thisNodePort = listener.getPortNumber();
        }
    }

    private void listenForTextInput() throws IOException {
        TextInputThread textInputThread = new TextInputThread(this);
        textInputThread.start();
    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        if (event instanceof NodeInformation) {
            Lookup lookup = new Lookup();
            lookup.setPayloadID(fileID);
            lookup.setRoutingPath(thisNodeHost + ":" + thisNodePort + ",");
        } else if (event instanceof DestinationNode) {
            FilePayload file = new FilePayload();
            file.setFileID(fileID);
            file.setFileName(filePath.getFileName().toString());
            file.setFileToTransfer(new File(filePath.toString()));

            TCPSender sender = new TCPSender();
            sender.sendToSpecificSocket(destinationSocket, file.getBytes());
        }
    }

    @Override
    public void processText(String text) throws IOException {
        String command = text.split("\\s")[0];
        switch (command) {
            case "file":
                filePath = Paths.get(text.split("\\s")[1]);
                file = new File(text.split("\\s")[1]);
                fileID = Integer.parseInt(text.split("\\s")[2]); //TODO: Make this be calculated automatically!
                StoreDataInquiry inquiry = new StoreDataInquiry();
                inquiry.setSixteenBitID(fileID);
                sender.sendToSpecificSocket(discoveryNodeSocket, inquiry.getBytes());
                break;
        }

    }

    public static void main(String[] args) {
        discoveryNodeHost = args[0];
        discoveryNodePort = Integer.parseInt(args[1]);

        try {
            StoreData storeData = new StoreData();
            storeData.startup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
