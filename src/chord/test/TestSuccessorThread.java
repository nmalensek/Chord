package chord.test;

import chord.messages.Query;
import chord.node.NodeRecord;
import chord.node.Peer;
import chord.transport.TCPSender;

import java.io.IOException;
import java.net.Socket;

public class TestSuccessorThread extends Thread {

        private QueryTest owner;
        private int queryInterval;
        private String ownerHost;
        private int ownerPort;
        TCPSender sender = new TCPSender();

        public TestSuccessorThread(QueryTest owner, int queryInterval, String ownerHost, int ownerPort) {
            this.owner = owner;
            this.queryInterval = queryInterval;
            this.ownerHost = ownerHost;
            this.ownerPort = ownerPort;
        }

        private void queryOwnerSuccessor() {
            try {
                NodeRecord successor = owner.getFingerTable().get(1);
                TestMessage query = new TestMessage();
                sender.sendToSpecificSocket(successor.getNodeSocket(), query.getBytes());
            } catch (IOException e) {
                System.out.println("Could not contact successor, message was not sent.");
            } catch (NullPointerException npe) {
                System.out.println("Successor: " + ownerHost + ":" + ownerPort);
            }

        }

        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(queryInterval);
                    queryOwnerSuccessor();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


