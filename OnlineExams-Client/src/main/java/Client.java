import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        String name = "Client";

        ClientConnection clientConnection = new ClientConnection(host, port, name);

        Scanner scanner = new Scanner(System.in);
        try {
            clientConnection.autoConnectForced();
            
            // Handshake
            String assignedName = clientConnection.handshake();
            if (assignedName == null) {
                System.out.println("Handshake failed.");
                return;
            }
            System.out.println("Connected as: " + assignedName);

            // Aspetta il segnale di START
            clientConnection.read();
            while (!clientConnection.isCommunicationStarted()) {
                System.out.println("Waiting for the start signal...");
                clientConnection.read();
            }
            System.out.println("Quiz started!");

            // Ciclo principale del quiz
            while (true) {
                Object receivedObject = clientConnection.read();
                
                if (clientConnection.isCommunicationEnded()) {
                    System.out.println("Quiz finished!");
                    break;
                }

                if (clientConnection.isQuestion(receivedObject)) {
                    Question question = clientConnection.getQuestion();
                    printQuestion(question);
                    String answer = getAnswer(scanner);
                    clientConnection.send(answer);
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("An error occurred: " + e.getMessage());
        } finally {
            clientConnection.close();
            scanner.close();
        }
    }

    private static void printQuestion(Question question) {
        System.out.println("\nQuestion: " + question.getText());
        List<String> options = question.getOptions();
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("%d) %s%n", i, options.get(i));
        }
    }

    private static String getAnswer(Scanner scanner) {
        System.out.print("Your answer (enter the number): ");
        return scanner.nextLine();
    }
}