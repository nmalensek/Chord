package util;

import node.Node;

import java.io.IOException;
import java.util.Scanner;

public class TextInputThread extends Thread {
    private Node node;

    public TextInputThread(Node node) {
        this.node = node;
    }

    public void run() {
        Scanner userInput = new Scanner(System.in);
        while (userInput.hasNextLine()) {
            String command = userInput.nextLine();
            try {
                node.processText(command);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
