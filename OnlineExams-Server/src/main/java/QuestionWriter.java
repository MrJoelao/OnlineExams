import java.io.*;
import java.util.*;

public class QuestionWriter {
    public static void writeQuestionsToFile(String filePath, List<Question> questions) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Question question : questions) {
                writer.write(question.getText());
                writer.newLine();
                
                writer.write(String.valueOf(question.getOptionsCount()));
                writer.newLine();
                
                for (String option : question.getOptions()) {
                    writer.write(option);
                    writer.newLine();
                }
                
                writer.write(String.valueOf(question.getCorrectAnswer()));
                writer.newLine();
                writer.newLine(); // Empty line between questions
            }
        }
    }

    public static Question createQuestionFromInput(Scanner scanner) {
        System.out.println("\nEnter the question text:");
        String text = scanner.nextLine();
        
        int optionsCount;
        while (true) {
            System.out.println("How many options do you want for this question? (minimum 2):");
            try {
                optionsCount = Integer.parseInt(scanner.nextLine());
                if (optionsCount >= 2) break;
                System.out.println("Please enter a number greater than or equal to 2");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number");
            }
        }
        
        List<String> options = new ArrayList<>();
        for (int i = 0; i < optionsCount; i++) {
            System.out.printf("Enter option %d: ", i);
            options.add(scanner.nextLine());
        }
        
        int correctAnswer;
        while (true) {
            System.out.printf("Enter the number of the correct answer (0-%d):\n", optionsCount - 1);
            try {
                correctAnswer = Integer.parseInt(scanner.nextLine());
                if (correctAnswer >= 0 && correctAnswer < optionsCount) break;
                System.out.printf("The number must be between 0 and %d\n", optionsCount - 1);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number");
            }
        }
        
        return new Question(text, options, correctAnswer);
    }
} 