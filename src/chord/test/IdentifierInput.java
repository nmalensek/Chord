package chord.test;

import chord.util.ComputeHash;
import chord.util.ConvertHex;
import chord.util.CreateIdentifier;

import java.io.IOException;

public class IdentifierInput {

    private static String identifier;
    private static String nanoIdentifier;
    private ConvertHex convertHex = new ConvertHex();

    private void printIDHash() throws IOException {
        String hashedID = ComputeHash.SHA1FromBytes(identifier.getBytes());
        byte[] hashByte = convertHex.convertHexToBytes(hashedID);
        int ID = java.nio.ByteBuffer.wrap(hashByte).getInt();
        short sixteenBit = (short) ((short)(ID & 0xffff) - ((ID & 0x8000) << 1));
        if (sixteenBit <= 32767 && sixteenBit > -32768) {
            int test;
            if (sixteenBit < 0) {
                test = sixteenBit + 65535;
            } else {
                test = sixteenBit;
            }
            System.out.println(test);
//            System.out.println(sixteenBit);
        } else {
            System.out.println("NOK");
        }
//        System.out.println("");
//        System.out.println("final ID: " + ID);
    }

    private void useMethod() {
        int testID = CreateIdentifier.createIdentifier(String.valueOf(System.currentTimeMillis()));
        System.out.println(testID);
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
//                identifierInput.printIDHash();
                identifierInput.useMethod();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (StringIndexOutOfBoundsException se) {

            }
        }
    }
}
