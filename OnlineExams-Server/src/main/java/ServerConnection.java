import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerConnection {
    private final ServerSocket serverSocket;

    // Costruttore che verifica se il port è valido e crea il ServerSocket
    public ServerConnection(int port) throws IOException {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("The port must be between 0 and 65535.");
        }
        serverSocket = new ServerSocket(port);
    }

    // Metodo per accettare una connessione, gestendo eventuali eccezioni
    public Socket acceptConnection() throws IOException {
        try {
            return serverSocket.accept();
        } catch (IOException e) {
            throw new IOException("Error accepting connection: " + e.getMessage());
        }
    }

    // Metodo per chiudere la connessione, gestendo eventuali eccezioni
    public void close() throws IOException {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new IOException("Error closing connection: " + e.getMessage());
        }
    }
}