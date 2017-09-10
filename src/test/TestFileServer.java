package test;

import messaging.Collision;
import messaging.Event;
import messaging.FilePayload;
import node.Node;
import transport.TCPSender;
import transport.TCPServerThread;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Paths;

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
            file.setFilepath(Paths.get("LexusVSV.pdf"));
            file.setFileToTransfer(new File("LexusVSV.pdf"));

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
