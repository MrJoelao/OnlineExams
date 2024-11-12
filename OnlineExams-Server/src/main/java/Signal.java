import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class Signal implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    public static final String START = "START";
    public static final String END = "END";
    public static final String INFO = "INFO";

    private final String type;
    private final String message;

    // Costruttore che accetta solo il tipo
    public Signal(String type) {
        // Verifica che il tipo non sia nullo o vuoto
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type cannot be null or empty");
        }
        // Inizializza il tipo e imposta il messaggio a null
        this.type = type;
        this.message = null;
    }

    // Costruttore che accetta tipo e messaggio
    public Signal(String type, String message) {
        // Verifica che il tipo non sia nullo o vuoto
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type cannot be null or empty");
        }
        // Inizializza il tipo e il messaggio
        this.type = type;
        this.message = message;
    }

    // Metodo per ottenere il tipo
    public String getType() {
        return type;
    }

    // Metodo per ottenere il messaggio
    public String getMessage() {
        return message;
    }

    // Metodo toString per rappresentazione testuale
    @Override
    public String toString() {
        return "Signal{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    // Metodo equals per confronto tra oggetti
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Signal)) return false;
        Signal signal = (Signal) o;
        return type.equals(signal.type) && Objects.equals(message, signal.message);
    }

    // Metodo hashCode per generare hash
    @Override
    public int hashCode() {
        return Objects.hash(type, message);
    }
}