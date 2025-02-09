package chord.util;

import chord.node.NodeRecord;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

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
            if (k >= maximumBitSize) {
                k = k % maximumBitSize;
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

    public synchronized void printFingerTable(HashMap<Integer, NodeRecord> fingerTable) {
        for (int i : fingerTable.keySet()) {
            System.out.println(i + "\t" + fingerTable.get(i).toString());
        }
    }

    public synchronized boolean predecessorIsLargest(HashMap<Integer, NodeRecord> knownNodes, int predecessorID) {
        int largest = predecessorID;
        for (int id : knownNodes.keySet()) {
            if (id > predecessorID) {
                largest = id;
            }
        }
        return largest == predecessorID;
    }

    /**
     * Constructs finger table for a node.
     * @param ID The ID of the node.
     * @param fingerTable The hashmap to store the finger table.
     * @param knownNodes All nodes in the overlay that this node's aware of. Node information's maintained
     *                   aggressively in this implementation, so it should contain all nodes in the overlay.
     */
    public synchronized void updateConcurrentFingerTable(int ID, ConcurrentHashMap<Integer, NodeRecord> fingerTable, ConcurrentHashMap<Integer, NodeRecord> knownNodes) {
        for (int i = 1; i < 17; i++) {
            int k = ID + (powersOfTwo.get(i)); //get the row value
            if (k >= maximumBitSize) {
                k = k % maximumBitSize;
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

    public synchronized void printConcurrentFingerTable(ConcurrentHashMap<Integer, NodeRecord> fingerTable) {
        for (int i : fingerTable.keySet()) {
            System.out.println(i + "\t" + fingerTable.get(i).toString());
        }
    }

    public synchronized boolean nodeIsLargestConcurrent(ConcurrentHashMap<Integer, NodeRecord> knownNodes, int nodeID) {
        int largest = nodeID;
        for (int id : knownNodes.keySet()) {
            if (id > nodeID) {
                largest = id;
            }
        }
        return largest == nodeID;
    }

    public synchronized boolean nodeIsSmallestConcurrent(ConcurrentHashMap<Integer, NodeRecord> knownNodes, int nodeID) {
        int smallest = nodeID;
        for (int id : knownNodes.keySet()) {
            if (id < nodeID) {
                smallest = id;
            }
        }
        return smallest == nodeID;
    }
}
