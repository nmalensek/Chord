package chord.util;

public class CreateIdentifier {

    private static ConvertHex convertHex = new ConvertHex();

    public static int createIdentifier(String stringValue) {
        String hashedID = ComputeHash.SHA1FromBytes(stringValue.getBytes());
        byte[] hashByte = convertHex.convertHexToBytes(hashedID);
        int ID = java.nio.ByteBuffer.wrap(hashByte).getInt();
        short shortID = (short) ((short)(ID & 0xffff) - ((ID & 0x8000) << 1));
        if (shortID < 0) {
            int positiveID = shortID + 65535;
            return positiveID;
        } else {
            return shortID;
        }
    }
}
