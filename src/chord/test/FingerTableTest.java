package chord.test;

import chord.node.NodeRecord;
import chord.util.CreateIdentifier;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
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
                + ":" + 12345, identifier, Inet4Address.getLocalHost().getHostName(), false);
        for (int i = 1; i < 17; i++) {
            fingerTable.put(i, thisNode);
//            System.out.println(i + "\t" + thisNode.getIdentifier());
        }
    }

    public HashMap<Integer, Integer> returnSquares() {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 1; i < 5; i++) {
            Double max = Math.pow(2, (i-1));
            int maxID = max.intValue();
            map.put(i, maxID);
//            System.out.println(i + "\t" + maxID);
        }
        return map;
    }

    private void processLookupLogicTest() {

    }

    private void constructExampleFingerTable(int[] testNodes, int testID) { //test with 4-bit table
        HashMap<Integer, Integer> resultMap = new HashMap<>();
        ArrayList<Integer> knownNodes = new ArrayList<>();

        for (int i : testNodes) {
            knownNodes.add(i);
        }

        HashMap<Integer, Integer> twoMap = new HashMap<>(returnSquares());

        for (int i = 1; i < 5; i++) { //for every row in the FT
            int k = testID + (twoMap.get(i)); //get the row value
            if (k >= 16) {
                k = k % 16;
            }
            int smallestID = 65535;
            if (knownNodes.contains(k)) {
                resultMap.put(i, k);
            } else {
                for (int nodeID : knownNodes) { //get the smallest value >= k
                    if (nodeID == 4 && k > testID) { //simulating storing at successor
                        resultMap.put(i, nodeID);
                        break;
                    } else {
                        for (int n : knownNodes) {
                            if (n > k && n < smallestID) {
                                smallestID = n;
                            }
                        }
                        resultMap.put(i, smallestID);
                    }
                }
            }
        }

        System.out.println("Results:");
        for (int row : resultMap.keySet()) {
            System.out.println(row + "\t" + resultMap.get(row));
        }
    }

    int[] testArray = {
      8, 11, 14
    };

    private void testFingerTable() {
        constructExampleFingerTable(testArray, 4);
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
