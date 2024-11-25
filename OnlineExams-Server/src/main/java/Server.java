import java.io.File;
import java.io.IOException;
import java.util.*;

public class Server {
    private static List<Question> loadedQuestions = null;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            printMenu();
            String choice = scanner.nextLine();
            
            try {
                switch (choice) {
                    case "1" -> startQuiz();
                    case "2" -> loadQuestions();
                    case "3" -> createQuestions();
                    case "4" -> {
                        System.out.println("Server terminated.");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            
            System.out.println("\nPress ENTER to continue...");
            scanner.nextLine();
        }
    }

    private static void printMenu() {
        System.out.println("\n=== SERVER MENU ===");
        System.out.println("1. Start quiz" + (loadedQuestions == null ? " (Load questions first)" : ""));
        System.out.println("2. Load questions from file");
        System.out.println("3. Create new questions file");
        System.out.println("4. Exit");
        System.out.print("Choice: ");
    }

    private static void startQuiz() throws IOException, InterruptedException {
        if (loadedQuestions == null) {
            System.out.println("You must load questions first!");
            return;
        }

        int port = 12345;
        ServerConnection serverConnection = new ServerConnection(port, loadedQuestions);
        System.out.println("Server started on port " + port);
        
        Thread serverThread = new Thread(serverConnection::startServer);
        serverThread.start();
        
        System.out.println("Press ENTER to start the quiz...");
        scanner.nextLine();
        
        serverConnection.startGame();
        System.out.println("Quiz started!");
        
        serverConnection.waitForAllClientsToFinish();
        
        List<Score> leaderboard = serverConnection.getScores();
        System.out.println("\n=== FINAL RANKINGS ===");
        for (int i = 0; i < leaderboard.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, leaderboard.get(i));
        }
        System.out.println("=====================");
        
        serverConnection.close();
    }

    private static void loadQuestions() {
        System.out.println("\nEnter the path to the questions file (or just the filename to use default directory):");
        String input = scanner.nextLine();
        
        try {
            String filePath = FileUtils.resolvePath(input);
            if (filePath == null) {
                System.out.println("Invalid file path.");
                return;
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("File does not exist: " + filePath);
                return;
            }
            
            loadedQuestions = QuestionParser.parseQuestionsFile(filePath);
            System.out.println("Loaded " + loadedQuestions.size() + " questions from: " + filePath);
            
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            loadedQuestions = null;
        }
    }

    private static void createQuestions() {
        List<Question> newQuestions = new ArrayList<>();
        
        while (true) {
            newQuestions.add(QuestionWriter.createQuestionFromInput(scanner));
            
            System.out.println("\nDo you want to add another question? (y/n)");
            if (!scanner.nextLine().toLowerCase().startsWith("y")) break;
        }
        
        System.out.println("\nEnter the filename to save (or full path):");
        String input = scanner.nextLine();
        
        try {
            String filePath;
            if (input.trim().isEmpty()) {
                // Se non viene fornito un nome, genera un nome file basato sulla data
                String defaultName = "quiz_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
                filePath = FileUtils.getDefaultPath(defaultName);
            } else {
                filePath = FileUtils.resolvePath(input);
            }
            
            if (filePath == null) {
                System.out.println("Invalid file path.");
                return;
            }
            
            QuestionWriter.writeQuestionsToFile(filePath, newQuestions);
            System.out.println("File created successfully at: " + filePath);
            
            System.out.println("Do you want to load these questions for a quiz? (y/n)");
            if (scanner.nextLine().toLowerCase().startsWith("y")) {
                loadedQuestions = newQuestions;
                System.out.println("Questions loaded!");
            }
            
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }
}