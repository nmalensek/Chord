package chord.util;

public class SplitHostPort {

    public String getHost(String hostPort) {
        return hostPort.split(":")[0];
    }

    public int getPort(String hostPort) {
        return Integer.parseInt(hostPort.split(":")[1]);
    }

    public int getID(String hostPortID) {
        return Integer.parseInt(hostPortID.split(":")[2]);
    }

    public String getHostPort(String hostPortID) { return hostPortID.split(":")[0] + ":" + hostPortID.split(":")[1]; }
}
