package transport;

import node.Node;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * code adapted from code provided by instructor at http://www.cs.colostate.edu/~cs455/lectures/CS455-HelpSession1.pdf
 */

public class TCPServerThread extends Thread {
    private Node node;
    private int portNum;
    private ServerSocket serverSocket;
    private ExecutorService pool = Executors.newFixedThreadPool(10);

    public TCPServerThread(Node node, int portNum) {
        this.node = node;
        this.portNum = portNum;
    }

    public int getPortNumber() { return portNum; }

    public void run() {
        try {
            serverSocket = new ServerSocket(portNum);
            portNum = serverSocket.getLocalPort();
            System.out.println("Server running on port " + serverSocket.getLocalPort() + "...");

            while(true) {
//                pool.execute(new TCPReceiverThread(serverSocket.accept(), node));
                new TCPReceiverThread(serverSocket.accept(), node).start();
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
