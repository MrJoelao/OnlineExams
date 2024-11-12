import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        try {
            ClientConnection clientConnection = new ClientConnection(host, port);
            System.out.println("Connected to server at " + host + ":" + port);

            try (ObjectOutputStream out = new ObjectOutputStream(clientConnection.getSocket().getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(clientConnection.getSocket().getInputStream())) {

                Scanner scanner = new Scanner(System.in);

                while (true) {
                    Object receivedObject = in.readObject();

                    if (receivedObject instanceof Signal) {
                        Signal signal = (Signal) receivedObject;
                        if (Signal.END.equals(signal.getType())) {
                            System.out.println("End of communication.");
                            break;
                        } else if (Signal.START.equals(signal.getType())) {
                            System.out.println("Start of questions.");
                            continue;
                        }
                    }

                    if (receivedObject instanceof Question) {
                        Question question = (Question) receivedObject;
                        System.out.println("Received question: " + question.getText());
                        System.out.println("Options: " + question.getOptions());

                        System.out.print("Your answer: ");
                        String answer = scanner.nextLine();

                        out.writeObject(answer);
                        out.flush();
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            clientConnection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}