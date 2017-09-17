package chord.util;

import java.util.HashMap;

public class FingerTableManagement {

    public static HashMap<Integer, Integer> returnSquares() {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 1; i < 17; i++) {
            Double maxDouble = Math.pow(2, (i-1));
            int maxID = maxDouble.intValue();
            map.put(i, maxID);
        }
        return map;
    }
}
