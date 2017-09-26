package chord.node;

import chord.messages.*;
import chord.transport.TCPSender;
import chord.transport.TCPServerThread;
import chord.util.CreateIdentifier;
import chord.util.SplitHostPort;
import chord.utilitythreads.TextInputThread;

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
    private SplitHostPort split = new SplitHostPort();
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
        TCPServerThread listener = new TCPServerThread(this, thisNodePort);
        listener.start();
    }

    private void listenForTextInput() throws IOException {
        TextInputThread textInputThread = new TextInputThread(this);
        textInputThread.start();
    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        if (event instanceof NodeInformation) { //gets random peer from DiscoveryNode to send lookup message
            Lookup lookup = new Lookup();
            lookup.setPayloadID(fileID);
            lookup.setRoutingPath(thisNodeHost + ":" + thisNodePort + ":" + 99999 + ",");
            String lookupHostPort = ((NodeInformation) event).getNodeInfo();
            Socket lookupSocket = new Socket(split.getHost(lookupHostPort), split.getPort(lookupHostPort));
            System.out.println("Sending lookup to " + ((NodeInformation) event).getNodeInfo());
            sender.sendToSpecificSocket(lookupSocket, lookup.getBytes());
        } else if (event instanceof DestinationNode) {
            DestinationNode destination = (DestinationNode) event;
            FilePayload file = new FilePayload();
            file.setFileID(fileID);
            file.setFileName("nmalensk_" + filePath.getFileName().toString());
            file.setFileToTransfer(new File(filePath.toString()));
            file.setSendingNodeHostPort(thisNodeHost + ":" + thisNodePort);
            System.out.println("Sending file " + fileID + " to " + destination.getDestinationNode());

            Socket fileReceiverSocket = new Socket(split.getHost(destination.getDestinationNode()),
                    split.getPort(destination.getDestinationNode()));

            TCPSender sender = new TCPSender();
            sender.sendToSpecificSocket(fileReceiverSocket, file.getBytes());
        } else if (event instanceof Collision) {
            System.out.println("A file already exists in the network with that ID. Specify a new ID or rename the file and try again.");
        }
    }

    @Override
    public void processText(String text) throws IOException {
        String command = text.split("\\s")[0];
        switch (command) {
            case "file":
                try {
                    filePath = Paths.get(text.split("\\s")[1]);
                    file = new File(text.split("\\s")[1]);
                    if (text.split("\\s")[2].equals("na")) {
                        fileID = CreateIdentifier.createIdentifier(filePath.getFileName().toString());
                    } else {
                        fileID = Integer.parseInt(text.split("\\s")[2]);
                        if (fileID < 0 || fileID > 65535) {
                            throw new NumberFormatException();
                        }
                    }
                    StoreDataInquiry inquiry = new StoreDataInquiry();
                    inquiry.setSixteenBitID(fileID);
                    sender.sendToSpecificSocket(discoveryNodeSocket, inquiry.getBytes());
                } catch (StringIndexOutOfBoundsException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    System.out.println("Could not generate ID from the supplied filename, please specify a new ID or rename and try again");
                    System.out.println("Usage: file [filePath] [optional fileID]");
                }
                break;
            default:
                System.out.println("Valid commands are: file [filePath] [optional fileID]");
                break;
        }

    }

    public static void main(String[] args) {
        discoveryNodeHost = args[0];
        discoveryNodePort = Integer.parseInt(args[1]);
        thisNodePort = Integer.parseInt(args[2]);

        try {
            try {
                StoreData storeData = new StoreData();
                storeData.startup();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: [DiscoveryNode host] [DiscoveryNode port] [StoreData port]");
        }
    }
}
