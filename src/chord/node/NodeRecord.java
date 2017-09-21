package chord.node;

import java.io.IOException;
import java.net.Socket;

public class NodeRecord {
    private String host;
    private int port;
    private int identifier;
    private String nickname;
    private Socket nodeSocket;

    public NodeRecord(String hostPort, int identifier, String nickname, Socket nodeSocket) throws IOException {
        this.host = hostPort.split(":")[0];
        this.port = Integer.parseInt(hostPort.split(":")[1]);
        this.identifier = identifier;
        this.nickname = nickname;
        this.nodeSocket = nodeSocket;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeRecord that = (NodeRecord) o;

        return identifier == that.identifier;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return this.getHost() + ":" + this.getPort() + ":" + this.getIdentifier();
    }
}
