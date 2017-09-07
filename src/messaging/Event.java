package messaging;

import java.io.IOException;

public interface Event<T> {

    T getType();
    int getMessageType();
    byte[] getBytes() throws IOException;
}
