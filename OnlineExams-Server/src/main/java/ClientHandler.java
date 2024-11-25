import java.io.IOException;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ClientHandler implements Runnable {
    private final String clientName;
    private final Socket clientSocket;
    private final ServerConnection serverConnection;
    private volatile boolean connected = true;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(String clientName, Socket clientSocket, ServerConnection serverConnection) {
        this.clientName = clientName;
        this.clientSocket = clientSocket;
        this.serverConnection = serverConnection;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            while (connected) {
                Object received = in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            if (connected) {
                System.out.println("Connection lost with client: " + clientName);
            }
        } finally {
            disconnect();
        }
    }

    // Metodo per inviare il messaggio "END" al client
    public void sendEndMessage() {
        if (out != null) {
            try {
                out.writeObject(ServerConnection.END);
                out.flush();
                System.out.println("Sent END to client: " + clientName);
            } catch (IOException e) {
                System.err.println("Error sending END to client " + clientName + ": " + e.getMessage());
            }
        } else {
            System.err.println("Cannot send END message. Output stream is null for client: " + clientName);
            disconnect();
        }
    }

    // Metodo per disconnettere il client
    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (!clientSocket.isClosed()) clientSocket.close();
            System.out.println("Client " + clientName + " disconnected successfully.");
        } catch (IOException e) {
            System.err.println("Error disconnecting client " + clientName + ": " + e.getMessage());
        }
    }

    // ... Altri metodi esistenti ...
} 