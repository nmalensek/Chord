package messaging;

public interface Protocol {
    int ENTER_OVERLAY = 0;
    int ENTRANCE_SUCCESSFUL = 1;
    int COLLISION = 2;
    int STORE_DATA_INQUIRY = 3;
    int LOOKUP = 4;
    int DESTINATION = 5;
    int EXIT_OVERLAY = 99;
}
