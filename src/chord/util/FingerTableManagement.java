package chord.util;

import chord.node.NodeRecord;

import java.util.HashMap;

public class FingerTableManagement {

    private HashMap<Integer, Integer> powersOfTwo = new HashMap<>(returnSquares());
    private int maximumBitSize = 65535;

    public static HashMap<Integer, Integer> returnSquares() {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 1; i < 17; i++) {
            Double maxDouble = Math.pow(2, (i-1));
            int maxID = maxDouble.intValue();
            map.put(i, maxID);
        }
        return map;
    }

    public synchronized void updateFingerTable(int ID, HashMap<Integer, NodeRecord> fingerTable, HashMap<Integer, NodeRecord> knownNodes) {
        for (int i = 1; i < 17; i++) {
            int k = ID + (powersOfTwo.get(i)); //get the row value
            if (k >= 32) {
                k = k % 32;
            }
            int smallestID = maximumBitSize;
            if (knownNodes.get(k) != null) {
                fingerTable.put(i, knownNodes.get(k));
            } else {
                for (int n : knownNodes.keySet()) { //get the smallest value >= k
                    if (n > k && n < smallestID) {
                        smallestID = n;
                    }
                }
                if (smallestID == maximumBitSize) { //no node has a larger id, wrap around to smallest node
                    for (int nodeID : knownNodes.keySet()) {
                        if (smallestID == maximumBitSize) {
                            smallestID = nodeID;
                        } else if (nodeID < smallestID) {
                            smallestID = nodeID;
                        }
                    }
                }
                fingerTable.put(i, knownNodes.get(smallestID));
            }
        }
    }
}
