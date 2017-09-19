package chord.test;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathsTest {

    public static void main(String[] args) throws UnknownHostException {
        Path test = Paths.get("~/foo/bar/test.jpg");
        System.out.println(test.getFileName().toString());
        String exampleFile = "/tmp/testimage1.jpg";
        String filename = exampleFile.substring(5, exampleFile.length());
        System.out.println(filename);
        System.out.println(Inet4Address.getLocalHost().getHostName());
    }
}
