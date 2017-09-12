package chord.util;

import chord.node.NodeRecord;
import chord.node.Peer;

import java.util.HashMap;

public class DiagnosticPrinterThread extends Thread {

    private Peer owner;
    private int diagnosticInterval;
    private HashMap<Integer, NodeRecord> fingerTableCopy = new HashMap<>();
    private HashMap<Integer, String> responsibleFilesCopy = new HashMap<>();

    public DiagnosticPrinterThread(Peer owner, int diagnosticInterval) {
        this.owner = owner;
        this.diagnosticInterval = diagnosticInterval;
    }

    private void printDiagnostics() {
        checkForCurrentFingerTableAndPrint();
        printSuccessorAndPredecessor();
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
    }

    private void printFingerTable() {
            System.out.println("Node finger table:");
            for (Integer key : fingerTableCopy.keySet()) {
                NodeRecord currentRecord = fingerTableCopy.get(key);
                System.out.printf(key + "\t" + currentRecord.getIdentifier()
                        + "\t" + currentRecord.getNickname() + "\n");
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
    }

    private void printResponsibleFiles() {
        System.out.println("Responsible for files:");
        for (Integer key : responsibleFilesCopy.keySet()) {
            System.out.printf(key + "\t" + responsibleFilesCopy.get(key) + "\n");
        }
    }

    private void printSuccessorAndPredecessor() {
        NodeRecord successor = fingerTableCopy.get(1);
        NodeRecord predecessor = owner.getPredecessor();
        System.out.printf("Successor: " + successor.getIdentifier() + ":" + successor.getNickname());
        System.out.println("Predecessor: " + predecessor.getIdentifier() + ":" + predecessor.getNickname());
    }

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(diagnosticInterval);
                printDiagnostics();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
