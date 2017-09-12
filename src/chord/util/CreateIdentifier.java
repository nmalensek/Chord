package chord.util;

public class CreateIdentifier {

    private static ConvertHex convertHex = new ConvertHex();

    public static short createIdentifier(String stringValue) {
        String hashedID = ComputeHash.SHA1FromBytes(stringValue.getBytes());
        byte[] hashByte = convertHex.convertHexToBytes(hashedID);
        int ID = java.nio.ByteBuffer.wrap(hashByte).getInt();
        return (short) ((short)(ID & 0xffff) - ((ID & 0x8000) << 1));
    }
}
