package chord.test;

import chord.messages.FilePayload;
import chord.transport.TCPSender;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FileStorageTest {

    private ConcurrentHashMap<Integer, String> filesResponsibleFor = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, TestNodeRecord> knownNodes = new ConcurrentHashMap<>();

    public FileStorageTest() throws IOException {
    }

    private void addTestNodes(int numNodes) {
        StringBuilder nodes = new StringBuilder("Known nodes: ");
        for (int i = 0; i < numNodes; i++) {
            int index = ThreadLocalRandom.current().nextInt(0, 10);
            knownNodes.put(testArray[index].getIdentifier(), testArray[index]);
            nodes.append(testArray[index].getIdentifier()).append(",");
        }
        System.out.println(nodes);
    }

    private void addTestFiles() {
        filesResponsibleFor.put(17, "ID17");
        filesResponsibleFor.put(22, "ID22");
        filesResponsibleFor.put(9, "ID9");
        filesResponsibleFor.put(3, "ID3");
        filesResponsibleFor.put(11, "ID11");
        filesResponsibleFor.put(1, "ID1");
    }

    private void storageTest(int ID) throws IOException { //new node joins overlay
        knownNodes.put(ID, new TestNodeRecord("test:1234", ID, "test"));
        for (int fileID : filesResponsibleFor.keySet()) {
            if (knownNodes.size() == 1 || fileID == ID) {
                System.out.println("Store at this node (ID " + ID + ")");
            } else {
             int smallest = 65535;
             for (int nodeID : knownNodes.keySet()) {
                 if (nodeID >= fileID && nodeID < smallest) {
                     smallest = nodeID;
                 }
             }
             if (smallest == 65535) {
                 System.out.println("File " + fileID + " stays at current node.");
             } else {
                 System.out.println("Store fileID " + fileID + " at ID " + smallest);
             }
            }
        }
    }

    TestNodeRecord[] testArray = {
            new TestNodeRecord("test:1234", 2, "test"),
            new TestNodeRecord("test:1234", 5, "test"),
            new TestNodeRecord("test:1234", 8, "test"),
            new TestNodeRecord("test:1234", 14, "test"),
            new TestNodeRecord("test:1234", 15, "test"),
            new TestNodeRecord("test:1234", 19, "test"),
            new TestNodeRecord("test:1234", 23, "test"),
            new TestNodeRecord("test:1234", 26, "test"),
            new TestNodeRecord("test:1234", 29, "test"),
            new TestNodeRecord("test:1234", 31, "test"),
    };

    private void simpleStorageTest(int currentNode, int newNode, int fileID) {
        int smallest = 65535;
        if (newNode < currentNode && newNode >= fileID || newNode > currentNode && newNode >= fileID) {
            System.out.println("Store ID " + fileID + " at " + newNode);
        } else {
            System.out.println("Store ID " + fileID + " at " + currentNode);
        }
    }

    public static void main(String[] args) throws IOException {
        FileStorageTest fileStorageTest = new FileStorageTest();
//        fileStorageTest.addTestNodes(2);
//        fileStorageTest.addTestFiles();
//        fileStorageTest.storageTest(15);
        fileStorageTest.simpleStorageTest(15, 8, 9);
        fileStorageTest.simpleStorageTest(15, 8, 6);
        fileStorageTest.simpleStorageTest(15, 6, 6);
        System.out.println("------");
        fileStorageTest.simpleStorageTest(3, 6, 9);
        fileStorageTest.simpleStorageTest(3, 6, 4);
    }
}
