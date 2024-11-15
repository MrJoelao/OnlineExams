import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String host;
    private final int port;
    private Object receivedObject;

    // Costruttore che inizializza host e port
    public ClientConnection(String host, int port) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("The host cannot be null or empty.");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("The port must be between 0 and 65535.");
        }
        this.host = host;
        this.port = port;
    }

    // Metodo per connettersi al server con tentativi di riconnessione
    public void autoConnect() {
        while (true) { // Loop per continuare a tentare la connessione
            try {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("Connected to server at " + host + ":" + port);
                break; // Esce dal loop se la connessione ha successo
            } catch (IOException e) {
                System.out.println("Unable to connect to server. Retrying...");
                try {
                    Thread.sleep(5000); // Aspetta 5 secondi prima di ritentare
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public boolean isEnd() {
        return receivedObject instanceof Signal && ((Signal) receivedObject).getType() == Signal.END;
    }

    public boolean isStart() {
        return receivedObject instanceof Signal && ((Signal) receivedObject).getType() == Signal.START;
    }

    public boolean isSignal() {
        return receivedObject instanceof Signal;
    }

    public boolean isQuestion(Object receivedObject) {
        return receivedObject instanceof Question;
    }

    public Question getQuestion() {
        if (!isQuestion(receivedObject)) {
            return null;
        }
        return (Question) receivedObject; 
    }

    // Metodo per inviare dati
    public void send(Object obj) throws IOException {
        if (obj == null) {
            throw new NullPointerException("The object to be sent cannot be null.");
        }
        out.writeObject(obj);
        out.flush();
    }
    
    // Metodo per ricevere dati
    public Object read() throws IOException, ClassNotFoundException {
        try {
            receivedObject = in.readObject();
            return receivedObject;
        } catch (EOFException e) {
            receivedObject = new Signal(Signal.END);
            return receivedObject;
        }
    }

    // Metodo per chiudere la connessione
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}