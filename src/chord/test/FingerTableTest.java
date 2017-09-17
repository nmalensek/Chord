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

    public FingerTableTest() throws IOException {
        identifier = CreateIdentifier.createIdentifier(String.valueOf(System.currentTimeMillis()));
    }

    private void constructInitialFingerTable() throws IOException {
        NodeRecord thisNode = new NodeRecord(Inet4Address.getLocalHost().getHostName()
                + ":" + 12345, identifier, Inet4Address.getLocalHost().getHostName(), false);
        for (int i = 1; i < 17; i++) {
            fingerTable.put(i, thisNode);
//            System.out.println(i + "\t" + thisNode.getIdentifier());
        }
    }

    public HashMap<Integer, Integer> returnSquares() {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 1; i < 6; i++) {
            Double max = Math.pow(2, (i-1));
            int maxID = max.intValue();
            map.put(i, maxID);
//            System.out.println(i + "\t" + maxID);
        }
        return map;
    }

    private void processLookupLogicTest() {

    }

    private void constructExampleFingerTable(NodeRecord[] testNodes, int testID) { //test with 5-bit table
        HashMap<Integer, NodeRecord> resultMap = new HashMap<>();
        HashMap<Integer, NodeRecord> knownNodes = new HashMap<>();

        for (NodeRecord testNode : testNodes) {
            knownNodes.put(testNode.getIdentifier(), testNode);
        }

        HashMap<Integer, Integer> twoMap = new HashMap<>(returnSquares());

        for (int i = 1; i < 6; i++) { //for every row in the FT
            int k = testID + (twoMap.get(i)); //get the row value
            if (k >= 32) {
                k = k % 32;
            }
            int smallestID = 65535;
            if (knownNodes.get(k) != null) {
                resultMap.put(i, knownNodes.get(k));
            } else {
                for (int n : knownNodes.keySet()) { //get the smallest value >= k
                            if (n > k && n < smallestID) {
                                smallestID = n;
                            }
                        }
                        if (smallestID == 65535) { //no node has a larger id, should wrap around to smallest node
                            for (int nodeID : knownNodes.keySet()) {
                                if (smallestID == 65535) {
                                    smallestID = nodeID;
                                } else if (nodeID < smallestID) {
                                    smallestID = nodeID;
                                }
                            }
                        }
                        resultMap.put(i, knownNodes.get(smallestID));
                }
            }
        System.out.println("Results for node " + testID + ":");
        for (int row : resultMap.keySet()) {
            System.out.println(row + "\t" + resultMap.get(row).getIdentifier());
        }

        }

    NodeRecord[] testArray = {
      new NodeRecord("test:1234", 2, "test", false),
      new NodeRecord("test:1234", 5, "test", false),
      new NodeRecord("test:1234", 8, "test", false),
      new NodeRecord("test:1234", 14, "test", false),
      new NodeRecord("test:1234", 15, "test", false),
      new NodeRecord("test:1234", 19, "test", false),
      new NodeRecord("test:1234", 23, "test", false),
      new NodeRecord("test:1234", 26, "test", false),
      new NodeRecord("test:1234", 29, "test", false),
      new NodeRecord("test:1234", 31, "test", false),
    };

    private void testFingerTable() {
        for (NodeRecord node : testArray) {
         constructExampleFingerTable(testArray, node.getIdentifier());
        }
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

    public static void main(String[] args) throws IOException {
        FingerTableTest fingerTableTest = new FingerTableTest();
        try {
            fingerTableTest.constructInitialFingerTable();
//            fingerTableTest.returnSquares();
            fingerTableTest.testFingerTable();
            while (true) {
                Thread.sleep(300);
//                fingerTableTest.comparisonTest();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
