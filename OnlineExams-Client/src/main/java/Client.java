import java.io.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        ClientConnection clientConnection = new ClientConnection(host, port);

        try {
            clientConnection.autoConnect();

            Scanner scanner = new Scanner(System.in);

            while (true) {
                // Riceve oggetto dal server
                clientConnection.read();


                if (clientConnection.isEnd()) {
                    System.out.println("End of communication.");
                    break;
                } else if (clientConnection.isStart()) {
                    System.out.println("Start of questions.");
                    continue;
                }

                Question question = clientConnection.getQuestion();

                if(question != null){
                    System.out.println("Received question: " + question.getText());
                    System.out.println("Options: " + question.getOptions());

                    System.out.print("Your answer: ");
                    String answer = scanner.nextLine();

                    clientConnection.send(answer);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                clientConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}