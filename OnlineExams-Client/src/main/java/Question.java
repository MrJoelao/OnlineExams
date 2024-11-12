import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Question implements Serializable {
    private final String text;
    private final List<String> options;
    private static final long serialVersionUID = 1L;

    public Question(String text, List<String> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("The list of options cannot be empty. Please provide at least one option for the question.");
        }
        this.text = text;
        this.options = Collections.unmodifiableList(options); // rende l'elenco delle opzioni immutabile
    }

    // Metodi getter
    public String getText() {
        return text;
    }

    public List<String> getOptions() {
        return options;
    }
}