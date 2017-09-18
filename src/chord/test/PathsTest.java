package chord.test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathsTest {

    public static void main(String[] args) {
        Path test = Paths.get("~/foo/bar/test.jpg");
        System.out.println(test.getFileName().toString());
        String exampleFile = "/tmp/testimage1.jpg";
        String filename = exampleFile.substring(5, exampleFile.length());
        System.out.println(filename);
    }
}
