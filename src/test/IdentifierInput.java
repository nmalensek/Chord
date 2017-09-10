package test;

import data.ConvertHex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IdentifierInput {

    private static String identifier;
    private static String nanoIdentifier;
    private ConvertHex convertHex = new ConvertHex();

    private void printIDHash() throws IOException {

        System.out.println(convertHex.convertBytesToHex(identifier.getBytes()));
        byte[] b = convertHex.convertHexToBytes("6e69636b");
        for (Byte kasf : b) {
            System.out.print(kasf);
        }
//        Path path = Paths.get("FullSizeRender.jpg");
//        byte[] picBytes = Files.readAllBytes(path);
//        String test = convertHex.convertBytesToHex(picBytes);
////        System.out.println(test);
//        System.out.println(Integer.decode(test));
//        String n = "nicholas";
//        System.out.println(Integer.decode(n));
//        byte[] stringToBytes = convertHex.convertHexToBytes("nicholas");
//        System.out.println(convertHex.convertBytesToHex(stringToBytes));
    }

    private void retryIdentifier() {
        try {
            Thread.sleep(5);
            identifier = String.valueOf(System.currentTimeMillis());
            nanoIdentifier = String.valueOf(System.nanoTime());
            printIDHash();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
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
//        input.retryIdentifier();
    }
}
