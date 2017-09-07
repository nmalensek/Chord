package node;

import java.io.IOException;

public class NodeRecord {
    private String host;
    private int port;
    private String identifier;
    private String nickname;

    public NodeRecord(String hostPort, String identifier, String nickname) throws IOException {
        this.host = hostPort.split(":")[0];
        this.port = Integer.parseInt(hostPort.split(":")[1]);
        this.identifier = identifier;
        this.nickname = nickname;
    }

    public String getHost() {
        return host;
    }

    public int getPort() { return port; }

    public String getIdentifier() { return identifier; }

    public String getNickname() { return nickname; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeRecord that = (NodeRecord) o;

        if (port != that.port) return false;
        return host != null ? host.equals(that.host) : that.host == null;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return this.getIdentifier();
    }
}
