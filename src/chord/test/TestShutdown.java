package chord.test;

import chord.messages.Event;
import chord.messages.NodeLeaving;
import chord.node.Node;
import chord.transport.TCPSender;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TestShutdown implements Node{

    private static TCPSender sender = new TCPSender();
    private String host = InetAddress.getLocalHost().getHostName();

    public TestShutdown() throws UnknownHostException {
    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {

    }

    @Override
    public void processText(String text) throws IOException {

    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Socket testSocket = new Socket(host, 60851);
                System.out.println("sending exit message...");
                NodeLeaving leaving = new NodeLeaving();
                leaving.setSixteenBitID(12345);
                sender.sendToSpecificSocket(testSocket, leaving.getBytes());
                System.out.println("exiting...");
                testSocket.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }));
    }

    public static void main(String[] args) throws UnknownHostException {
        TestShutdown testShutdown = new TestShutdown();
        testShutdown.addShutdownHook();
        while (true) {

        }
    }
}
