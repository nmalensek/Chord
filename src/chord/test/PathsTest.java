package chord.test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathsTest {

    public static void main(String[] args) {
        Path test = Paths.get("~/foo/bar/chord.messaging.test.jpg");
        System.out.println(test.getFileName().toString());
    }
}
