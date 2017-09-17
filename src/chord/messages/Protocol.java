package chord.messages;

public interface Protocol {
    int ENTER_OVERLAY = 0;
    int ENTRANCE_SUCCESSFUL = 1;
    int COLLISION = 2;
    int STORE_DATA_INQUIRY = 3;
    int LOOKUP = 4;
    int DESTINATION = 5;
    int FILE = 6;
    int QUERY = 7;
    int QUERY_RESPONSE = 8;
    int UPDATE = 9;
    int ASK_FOR_SUCCESSOR = 10;
    int SUCCESSOR_INFO = 11;
    int EXIT_OVERLAY = 99;
}
