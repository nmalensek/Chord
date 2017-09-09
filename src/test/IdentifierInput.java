package test;

import data.ConvertHex;

public class IdentifierInput {

    private static String identifier;
    private static String nanoIdentifier;
    private ConvertHex convertHex = new ConvertHex();

    private void printIDHash() {
        String hexString = convertHex.convertBytesToHex(identifier.getBytes());
        String nanoHex = convertHex.convertBytesToHex(nanoIdentifier.getBytes());
        System.out.println("identifier converted to hex: " + hexString);
        System.out.println("nanotime converted to hex: " + nanoHex);
    }

    private void retryIdentifier() {
        try {
            Thread.sleep(5);
            identifier = String.valueOf(System.currentTimeMillis());
            nanoIdentifier = String.valueOf(System.nanoTime());
            printIDHash();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            identifier = args[0];
            nanoIdentifier = String.valueOf(System.nanoTime());
        } else {
            identifier = String.valueOf(System.currentTimeMillis());
            nanoIdentifier = String.valueOf(System.nanoTime());
        }
        System.out.println("argument or milliseconds: " + identifier);
        System.out.println("nanotime: " + nanoIdentifier);
        IdentifierInput input = new IdentifierInput();
        input.printIDHash();
        input.retryIdentifier();
    }
}
