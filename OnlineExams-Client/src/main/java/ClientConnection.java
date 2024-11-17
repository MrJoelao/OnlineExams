import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String host;
    private final int port;
    private Object receivedObject;
    private final String name;

    // Costanti per i tipi di segnale
    public static final String START = "START";
    public static final String END = "END";
    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";

    // Costruttore che inizializza host e port
    public ClientConnection(String host, int port, String name) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("The host cannot be null or empty.");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("The port must be between 0 and 65535.");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("The name cannot be null or empty.");
        }

        this.name = name;
        this.host = host;
        this.port = port;
    }

    // Metodo per connettersi al server con tentativi di riconnessione usando il thread sleep forzando il processo ad aspettare
    public void autoConnectForced() {
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

    // Metodo per connettersi al server in modo asincrono con tentativi di riconnessione
    public void autoConnectAsync() {
        new Thread(() -> {
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
        }).start();
    }

    public String handshake() throws IOException, ClassNotFoundException {
        send(name);
        read();
        if (!isString(receivedObject) || !receivedObject.equals(SUCCESS)) {
            return null;
        }
        
        // Leggi il nome univoco assegnato dal server
        read();
        if (!isString(receivedObject)) {
            return null;
        }
        
        return (String) receivedObject;
    }

    public boolean isCommunicationEnded() {
        if(!isString(receivedObject)) {
            return false;
        }
        
        String message = (String) receivedObject;
        System.out.println("Received string message: " + message);
        return message.equals(END);
    }

    public boolean isCommunicationStarted() {
        if(!isString(receivedObject)) {
            return false;
        }
        
        String message = (String) receivedObject;
        System.out.println("Received string message: " + message);
        return message.equals(START);
    }

    public boolean isQuestion(Object receivedObject) {
        boolean result = receivedObject instanceof Question;
        if (result) {
            System.out.println("Received object is a Question.");
        } else {
            System.out.println("Received object is not a Question.");
        }
        return result;
    }

    public Question getQuestion() {
        if (!isQuestion(receivedObject)) {
            return null;
        }
        return (Question) receivedObject;
    }

    public boolean isString(Object receivedObject) {
        boolean result = receivedObject instanceof String;
        if (result) {
            System.out.println("Received object is a String.");
            return true;
        }
        System.out.println("Received object is not a String.");
        return false;
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
        } catch (EOFException e) {
            receivedObject = ERROR;
        }
        return receivedObject;
    }

    // Metodo per chiudere la connessione
    public void close() {
        closeIn();
        closeOut();
        closeSocket();
    }

    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Failed to close Socket: " + e.getMessage());
            }
        }
    }

    private void closeIn() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                System.err.println("Failed to close ObjectInputStream: " + e.getMessage());
            }
        }
    }

    private void closeOut() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                System.err.println("Failed to close ObjectOutputStream: " + e.getMessage());
            }
        }
    }
}