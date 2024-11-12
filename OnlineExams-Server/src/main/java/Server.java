import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import static java.util.Arrays.*;

public class Server {
    public static void main(String[] args) {
        int port = 12345;
        try {
            ServerConnection serverConnection = new ServerConnection(port);
            System.out.println("Server started on port " + port);

            List<Question> questions = asList(
                new Question("First Question?", asList("Option 1", "Option 2", "Option 3", "Option 4"), 1),
                new Question("Second Question?", asList("Option A", "Option B", "Option C", "Option D"), 2)
                // Add more questions as needed
            );

            while (true) {
                Socket clientSocket = serverConnection.acceptConnection();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                     ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

                    // Send start signal
                    out.writeObject(new Signal(Signal.START));
                    out.flush();

                    for (Question question : questions) {
                        out.writeObject(question);
                        out.flush();
                        System.out.println("Question sent: " + question.getText());

                        String response = (String) in.readObject();
                        System.out.println("Received response: " + response);
                    }

                    // Send end signal
                    out.writeObject(new Signal(Signal.END));
                    out.flush();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}