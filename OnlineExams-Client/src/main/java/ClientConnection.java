import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private final Socket socket;

    // Costruttore che verifica se l'host e il port sono validi e crea la connessione
    public ClientConnection(String host, int port) throws IOException {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("The host cannot be null or empty.");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("The port must be between 0 and 65535.");
        }
        socket = new Socket(host, port);
    }

    // Metodo per ottenere il socket
    public Socket getSocket() {
        return socket;
    }

    // Metodo per inviare dati, gestendo eventuali eccezioni
    public void send(Object obj) throws IOException {
        if (obj == null) {
            throw new NullPointerException("The object to be sent cannot be null.");
        }
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(obj);
            out.flush();
        } catch (IOException e) {
            throw new IOException("Error sending data: " + e.getMessage());
        }
    }

    // Metodo per ricevere dati, gestendo eventuali eccezioni
    public Object receive() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            return in.readObject();
        } catch (IOException e) {
            throw new IOException("Error receiving data: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Class not found during data reception: " + e.getMessage());
        }
    }

    // Metodo per chiudere la connessione, gestendo eventuali eccezioni
    public void close() throws IOException {
        try {
            socket.close();
        } catch (IOException e) {
            throw new IOException("Error closing connection: " + e.getMessage());
        }
    }
}