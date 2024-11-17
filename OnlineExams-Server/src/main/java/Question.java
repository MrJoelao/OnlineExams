import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Question implements Serializable {
    private final String text;
    private final List<String> options;
    private final int correctAnswer;
    @Serial
    private static final long serialVersionUID = 1L;

    public Question(String text, List<String> options, int correctAnswer) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("The list of options cannot be empty. Please provide at least one option for the question.");
        }
        if (correctAnswer < 0 || correctAnswer >= options.size()) {
            throw new IllegalArgumentException("The index of the correct answer is invalid. It must be between 0 and " + (options.size() - 1) + ", inclusive.");
        }
        this.text = text;
        this.options = Collections.unmodifiableList(options);
        this.correctAnswer = correctAnswer;
    }

    public String getText() {
        return text;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectAnswer() {
        return correctAnswer;
    }

    public int getOptionsCount() {
        return options.size();
    }
}