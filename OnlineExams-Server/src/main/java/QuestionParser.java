import java.io.*;
import java.util.*;

public class QuestionParser {
    public static List<Question> parseQuestionsFile(String filePath) throws IOException {
        List<Question> questions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                // Read the question text
                String questionText = line.trim();
                
                // Read the number of options
                line = reader.readLine();
                if (line == null) throw new IOException("File format error: missing options count");
                int optionsCount = Integer.parseInt(line.trim());
                
                // Read the options
                List<String> options = new ArrayList<>();
                for (int i = 0; i < optionsCount; i++) {
                    line = reader.readLine();
                    if (line == null) throw new IOException("File format error: missing options");
                    options.add(line.trim());
                }
                
                // Read the correct answer
                line = reader.readLine();
                if (line == null) throw new IOException("File format error: missing correct answer");
                int correctAnswer = Integer.parseInt(line.trim());
                
                questions.add(new Question(questionText, options, correctAnswer));
                
                // Skip the empty line between questions
                reader.readLine();
            }
        } catch (NumberFormatException e) {
            throw new IOException("File format error: invalid number format");
        }
        
        if (questions.isEmpty()) {
            throw new IOException("No questions found in file");
        }
        
        return questions;
    }
} 