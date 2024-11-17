import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class ServerGUI extends JFrame {
    private JPanel mainPanel;
    private JButton loadQuestionsButton;
    private JButton createQuestionsButton;
    private JButton startQuizButton;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JButton exitButton;
    
    private List<Question> loadedQuestions = null;
    private ServerConnection serverConnection = null;
    private boolean quizRunning = false;

    public ServerGUI() {
        setupFrame();
        setupLogging();
        setupListeners();
    }

    private void setupFrame() {
        setTitle("Online Exams Server");
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void setupLogging() {
        // Redirect System.out to logArea
        PrintStream printStream = new PrintStream(new CustomOutputStream(logArea));
        System.setOut(printStream);
        System.setErr(printStream);
    }

    private void setupListeners() {
        loadQuestionsButton.addActionListener(e -> loadQuestions());
        createQuestionsButton.addActionListener(e -> createQuestions());
        startQuizButton.addActionListener(e -> startQuiz());
        exitButton.addActionListener(e -> exitApplication());
    }

    private void loadQuestions() {
        JFileChooser fileChooser = new JFileChooser(FileUtils.getDefaultPath(""));
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = fileChooser.getSelectedFile();
                loadedQuestions = QuestionParser.parseQuestionsFile(selectedFile.getPath());
                updateStatus("Loaded " + loadedQuestions.size() + " questions");
                startQuizButton.setEnabled(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error loading questions: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createQuestions() {
        QuestionCreatorDialog dialog = new QuestionCreatorDialog(this);
        dialog.setVisible(true);
        
        if (dialog.getQuestions() != null) {
            loadedQuestions = dialog.getQuestions();
            updateStatus("Created " + loadedQuestions.size() + " questions");
            startQuizButton.setEnabled(true);
        }
    }

    private void startQuiz() {
        if (loadedQuestions == null || loadedQuestions.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please load questions first", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int port = 12345;
            serverConnection = new ServerConnection(port, loadedQuestions);
            
            // Disable buttons during quiz
            setButtonsEnabled(false);
            quizRunning = true;
            updateStatus("Server started on port " + port);
            
            // Start server in background
            new Thread(() -> {
                try {
                    serverConnection.startServer();
                } catch (Exception ex) {
                    if (quizRunning) {
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(this, 
                                "Server error: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE));
                    }
                }
            }).start();

            // Wait for clients
            int option = JOptionPane.showConfirmDialog(this,
                "Start the quiz when all clients are connected?",
                "Start Quiz", JOptionPane.YES_NO_OPTION);
                
            if (option == JOptionPane.YES_OPTION) {
                serverConnection.startGame();
                updateStatus("Quiz in progress...");
                
                // Wait for quiz completion in background
                new Thread(() -> {
                    try {
                        serverConnection.waitForAllClientsToFinish();
                        displayResults();
                        cleanup();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            } else {
                cleanup();
            }
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error starting server: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            cleanup();
        }
    }

    private void displayResults() {
        SwingUtilities.invokeLater(() -> {
            List<Score> leaderboard = serverConnection.getScores();
            StringBuilder results = new StringBuilder("\n=== FINAL RANKINGS ===\n");
            for (int i = 0; i < leaderboard.size(); i++) {
                results.append(String.format("%d. %s%n", i + 1, leaderboard.get(i)));
            }
            results.append("=====================\n");
            logArea.append(results.toString());
        });
    }

    private void cleanup() {
        try {
            if (serverConnection != null) {
                serverConnection.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        quizRunning = false;
        setButtonsEnabled(true);
        updateStatus("Quiz finished");
    }

    private void exitApplication() {
        if (quizRunning) {
            int option = JOptionPane.showConfirmDialog(this,
                "A quiz is currently running. Are you sure you want to exit?",
                "Exit Confirmation", JOptionPane.YES_NO_OPTION);
            if (option != JOptionPane.YES_OPTION) {
                return;
            }
        }
        cleanup();
        dispose();
        System.exit(0);
    }

    private void setButtonsEnabled(boolean enabled) {
        loadQuestionsButton.setEnabled(enabled);
        createQuestionsButton.setEnabled(enabled);
        startQuizButton.setEnabled(enabled);
        exitButton.setEnabled(enabled);
    }

    private void updateStatus(String status) {
        statusLabel.setText("Status: " + status);
    }

    private static class CustomOutputStream extends java.io.OutputStream {
        private final JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            SwingUtilities.invokeLater(() -> textArea.append(String.valueOf((char)b)));
        }
    }
}
