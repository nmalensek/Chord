package chord.utilitythreads;

import chord.node.NodeRecord;
import chord.node.Peer;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class DiagnosticPrinterThread extends Thread {

    private Peer owner;
    private int diagnosticInterval;
    private ConcurrentHashMap<Integer, NodeRecord> fingerTableCopy = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> responsibleFilesCopy = new ConcurrentHashMap<>();

    public DiagnosticPrinterThread(Peer owner, int diagnosticInterval) {
        this.owner = owner;
        this.diagnosticInterval = diagnosticInterval;
    }

    private void printDiagnostics() {
        checkForCurrentFingerTableAndPrint();
        printPredecessor();
        checkForCurrentFilesAndPrint();
    }

    private void checkForCurrentFingerTableAndPrint() {
        if (owner.isFingerTableModified()) {
            updateFTCopy();
            printFingerTable();
        } else {
            printFingerTable();
        }
    }

    private void updateFTCopy() {
        fingerTableCopy = owner.getFingerTable();
        owner.setFingerTableModified(false);
    }

    private void printFingerTable() {
            System.out.println("Node finger table:");
            for (Integer key : fingerTableCopy.keySet()) {
                NodeRecord currentRecord = fingerTableCopy.get(key);
                System.out.printf(key + "\t" + currentRecord.getIdentifier()
                        + "\t" + currentRecord.getNickname() + ":" + currentRecord.getPort() + "\n");
            }
    }

    private void checkForCurrentFilesAndPrint() {
        if (owner.isFilesResponsibleForModified()) {
            updateFilesCopy();
            printResponsibleFiles();
        } else {
            printResponsibleFiles();
        }
    }

    private void updateFilesCopy() {
        responsibleFilesCopy = owner.getFilesResponsibleFor();
        owner.setFilesResponsibleForModified(false);
    }

    private void printResponsibleFiles() {
        System.out.println("Responsible for files:");
        for (Integer key : responsibleFilesCopy.keySet()) {
            System.out.printf(key + "\t" + responsibleFilesCopy.get(key) + "\n");
        }
    }

    private void printPredecessor() {
        NodeRecord predecessor = owner.getPredecessor();
        System.out.println("Predecessor: " + predecessor.getIdentifier() +
                "\t" + predecessor.getNickname() + "\t" + predecessor.getPort());
    }

    //TODO find out why predecessor's ID is being set to its port sometimes

    @Override
    public void run() {
//        try {
//            while (true) {
//                Thread.sleep(diagnosticInterval);
//                printDiagnostics();
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }
}
