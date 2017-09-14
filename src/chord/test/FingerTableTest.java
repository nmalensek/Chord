package chord.test;

import chord.node.NodeRecord;
import chord.util.CreateIdentifier;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.HashMap;

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

    public static void main(String[] args) {
        FingerTableTest fingerTableTest = new FingerTableTest();
        try {
            fingerTableTest.constructInitialFingerTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
