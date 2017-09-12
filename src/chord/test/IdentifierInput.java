package chord.test;

import chord.util.ComputeHash;
import chord.util.ConvertHex;

import java.io.IOException;

public class IdentifierInput {

    private static String identifier;
    private static String nanoIdentifier;
    private ConvertHex convertHex = new ConvertHex();

    private void printIDHash() throws IOException {
        String hashedID = ComputeHash.SHA1FromBytes(identifier.getBytes());
//        System.out.println("Hashed ID: " + hashedID);
        byte[] hashByte = convertHex.convertHexToBytes(hashedID);
//        for (byte b : hashByte) {
//            System.out.print(b);
//        }
//        System.out.println("");
        int ID = java.nio.ByteBuffer.wrap(hashByte).getInt();
        short sixteenBit = (short) ((short)(ID & 0xffff) - ((ID & 0x8000) << 1));
//        sixteenBit = (short) Math.abs(sixteenBit);
        if (sixteenBit <= 32767 && sixteenBit > -32768) {
            System.out.println(sixteenBit);
        } else {
            System.out.println("NOK");
        }
//        System.out.println("");
//        System.out.println("final ID: " + ID);
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            identifier = args[0];
        } else {
            identifier = String.valueOf(System.currentTimeMillis());
        }
        IdentifierInput identifierInput = new IdentifierInput();
        while (true) {
            try {
                identifier = String.valueOf(System.currentTimeMillis());
                identifierInput.printIDHash();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (StringIndexOutOfBoundsException se) {

            }
        }
    }
}
