package chord.util;

public class SplitHostPort {

    public String getHost(String hostPort) {
        return hostPort.split(":")[0];
    }

    public int getPort(String hostPort) {
        return Integer.parseInt(hostPort.split(":")[1]);
    }
}
