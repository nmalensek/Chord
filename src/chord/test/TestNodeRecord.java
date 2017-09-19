package chord.test;

import java.io.IOException;
import java.net.Socket;

public class TestNodeRecord {
    private String host;
    private int port;
    private int identifier;
    private String nickname;
    private Socket nodeSocket;

    public TestNodeRecord(String hostPort, int identifier, String nickname) throws IOException {
        this.host = hostPort.split(":")[0];
        this.port = Integer.parseInt(hostPort.split(":")[1]);
        this.identifier = identifier;
        this.nickname = nickname;
    }

    public String getHost() {
        return host;
    }

    public int getPort() { return port; }

    public int getIdentifier() { return identifier; }

    public String getNickname() { return nickname; }

    public Socket getNodeSocket() { return nodeSocket; }

    public void setNodeSocket(Socket nodeSocket) {
        this.nodeSocket = nodeSocket;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return this.getNickname();
    }
}
