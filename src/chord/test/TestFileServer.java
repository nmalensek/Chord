package chord.test;

import chord.messages.Collision;
import chord.messages.Event;
import chord.messages.FilePayload;
import chord.node.Node;
import chord.transport.TCPSender;
import chord.transport.TCPServerThread;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

public class TestFileServer implements Node {


    private void startup() {
        TCPServerThread serverThread = new TCPServerThread(this, 53000);
        serverThread.start();
    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        if (event instanceof Collision) {
            System.out.println("sending file");
            FilePayload file = new FilePayload();
            file.setFileID(123456);
            file.setFileToTransfer(new File("123456.png"));
            file.setFileName("123456.png");

            TCPSender sender = new TCPSender();
            sender.sendToSpecificSocket(destinationSocket, file.getBytes());
            System.out.println("file sent");
//            System.out.println("got a collision");
//            Collision collision = new Collision();
//            TCPSender sender = new TCPSender();
//            sender.sendToSpecificSocket(destinationSocket, collision.getBytes());
//            System.out.println("sent collision");
        }
    }

    @Override
    public void processText(String text) throws IOException { }

    public static void main(String[] args) {
        TestFileServer testFileServer = new TestFileServer();
        testFileServer.startup();
    }
}
