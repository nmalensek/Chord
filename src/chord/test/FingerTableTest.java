package chord.test;

import chord.node.NodeRecord;
import chord.util.CreateIdentifier;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FingerTableTest {

    private HashMap<Integer, NodeRecord> fingerTable = new HashMap<>();
    private int identifier;

    public FingerTableTest() {
        identifier = CreateIdentifier.createIdentifier(String.valueOf(System.currentTimeMillis()));
    }

    private void constructInitialFingerTable() throws IOException {
        NodeRecord thisNode = new NodeRecord(Inet4Address.getLocalHost().getHostName()
                + ":" + 12345, identifier, Inet4Address.getLocalHost().getHostName());
        for (int i = 1; i < 17; i++) {
            fingerTable.put(i, thisNode);
            System.out.println(i + "\t" + thisNode.getIdentifier());
        }
    }

    private void processLookupLogicTest() {

    }

    private void constructExampleFingerTable() {

    }

    private void comparisonTest() {
        int IdA = ThreadLocalRandom.current().nextInt(0, 16);
        int IdB = ThreadLocalRandom.current().nextInt(0, 16);
        int larger = IdA > IdB ? IdA : IdB;
        int smaller = IdA < IdB ? IdA : IdB;
        int k = ThreadLocalRandom.current().nextInt(0, 16);

        if (k > larger || k < smaller) {
            System.out.println(k + " successor is " + smaller);
        } else if (smaller < k && k < larger) {
            System.out.println(k + " successor is " + larger);
        } else {
            System.out.println(k + " failed to be placed.");
        }
        System.out.println("smaller: " + smaller);
        System.out.println("larger: " + larger);
        System.out.println("----------------------------");
    }

    //TODO also create several types of lookup classes (if we need to retrieve files from nodes to nodes)!

    public static void main(String[] args) {
        FingerTableTest fingerTableTest = new FingerTableTest();
        try {
            fingerTableTest.constructInitialFingerTable();
            while (true) {
                Thread.sleep(300);
                fingerTableTest.comparisonTest();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
