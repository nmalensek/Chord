package test;

import messaging.Collision;
import messaging.Event;
import messaging.FilePayload;
import node.Node;
import transport.TCPReceiverThread;
import transport.TCPSender;
import transport.TCPServerThread;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;

public class TestFileReceiver implements Node{

    private static int serverPort = 53000;

    private void startup() throws IOException {
        Socket serverSocket = new Socket(Inet4Address.getLocalHost().getHostName(), serverPort);
        TCPReceiverThread serverReceiver = new TCPReceiverThread(serverSocket, this);
        serverReceiver.start();

        Collision collision = new Collision();
        System.out.println("sending collision");
        TCPSender sender = new TCPSender();
        sender.sendToSpecificSocket(serverSocket, collision.getBytes());
        System.out.println("sent collision");
    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        System.out.println("got a message");
        if (event instanceof FilePayload) {
            System.out.println("receiving file...");
            ((FilePayload) event).writeFile(((FilePayload) event).getFileByteArray(), "1234567.jpg");
            System.out.println("file received");
        } else if (event instanceof Collision) {
            System.out.println("server message received");
        }
    }

    @Override
    public void processText(String text) throws IOException {

    }

    public static void main(String[] args) throws IOException {
        TestFileReceiver testFileReceiver = new TestFileReceiver();
        testFileReceiver.startup();
    }
}
