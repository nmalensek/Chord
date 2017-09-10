package test;

import messaging.Event;
import messaging.NodeLeaving;
import node.Node;
import node.NodeRecord;
import transport.TCPServerThread;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class TestServer implements Node{

    private Map<String, NodeRecord> nodeMap = new HashMap<>();

    private void startServer() {
        TCPServerThread testServerThread = new TCPServerThread(this, 60851);
        testServerThread.start();
    }

    @Override
    public void onEvent(Event event, Socket destinationSocket) throws IOException {
        if (event instanceof NodeLeaving) {
            System.out.println("A node is leaving.");
        }
    }

    @Override
    public void processText(String text) throws IOException {

    }

    public static void main(String[] args) {
        TestServer testServer = new TestServer();
        testServer.startServer();
    }
}
