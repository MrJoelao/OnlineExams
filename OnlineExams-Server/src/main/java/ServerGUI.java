import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import com.formdev.flatlaf.FlatDarculaLaf;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.DefaultListModel;
import javax.swing.JList;

public class ServerGUI extends JFrame {
    private JPanel mainPanel;
    private JButton loadQuestionsButton;
    private JButton createQuestionsButton;
    private JButton startQuizButton;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JButton exitButton;
    private JButton changePortButton;
    private JLabel portLabel;
    
    // Nuovi componenti per la gestione dei client
    private JList<String> clientsList;
    private DefaultListModel<String> clientsListModel;
    private JButton removeClientButton;
    
    private List<Question> loadedQuestions = null;
    private ServerConnection serverConnection = null;
    private boolean quizRunning = false;
    private int port = 12345;

    public ServerGUI() {
        setLookAndFeel();
        setupFrame();
        setupLogging();
        setupListeners();
        initializeClientsList(); // Inizializza la lista dei client
        updatePortLabel();
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception e) {
            System.err.println("FlatLaf Darcula look and feel not available, using default.");
        }
    }

    private void setupFrame() {
        setTitle("Online Exams Server");
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void setupLogging() {
        // Reindirizza System.out a logArea
        PrintStream printStream = new PrintStream(new CustomOutputStream(logArea));
        System.setOut(printStream);
        System.setErr(printStream);
    }

    private void initializeClientsList() {
        clientsListModel = new DefaultListModel<>();
        clientsList.setModel(clientsListModel);
        refreshClientsList();
    }

    private void refreshClientsList() {
        clientsListModel.clear();
        List<String> connectedClients = serverConnection != null ? serverConnection.getConnectedClientNames() : Collections.emptyList();
        for (String clientName : connectedClients) {
            clientsListModel.addElement(clientName);
        }
    }

    private void setupListeners() {
        loadQuestionsButton.addActionListener(e -> loadQuestions());
        createQuestionsButton.addActionListener(e -> createQuestions());
        startQuizButton.addActionListener(e -> startQuiz());
        exitButton.addActionListener(e -> exitApplication());
        changePortButton.addActionListener(e -> changePort());
        removeClientButton.addActionListener(e -> removeSelectedClient()); // Listener per il nuovo pulsante
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
            serverConnection = new ServerConnection(port, loadedQuestions);
            serverConnection.setGui(this); // Imposta il riferimento alla GUI per aggiornamenti
            // Disabilita i pulsanti durante il quiz, eccetto removeClientButton
            setButtonsEnabled(false);
            quizRunning = true;
            updateStatus("Server started on port " + port);

            // Abilita solo removeClientButton
            removeClientButton.setEnabled(true);

            // Avvia il server in background
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

            // Attendi i client
            int option = JOptionPane.showConfirmDialog(this,
                "Start the quiz when all clients are connected?",
                "Start Quiz", JOptionPane.YES_NO_OPTION);
                
            if (option == JOptionPane.YES_OPTION) {
                serverConnection.startGame();
                updateStatus("Quiz in progress...");
                
                // Attendi il completamento del quiz in background
                new Thread(() -> {
                    try {
                        serverConnection.waitForAllClientsToFinish();
                        displayResults();
                        cleanup();
                        refreshClientsList();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            } else {
                cleanup();
                refreshClientsList();
            }
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error starting server: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            cleanup();
        }
    }

    private void removeSelectedClient() {
        String selectedClient = clientsList.getSelectedValue();
        if (selectedClient == null) {
            JOptionPane.showMessageDialog(this, "Please select a client to remove.", "No Client Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to remove client: " + selectedClient + "?",
            "Confirm Removal", JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            boolean success = serverConnection.removeClient(selectedClient);
            if (success) {
                JOptionPane.showMessageDialog(this, "Client " + selectedClient + " has been removed.", "Client Removed", JOptionPane.INFORMATION_MESSAGE);
                refreshClientsList();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to remove client: " + selectedClient, "Error", JOptionPane.ERROR_MESSAGE);
            }
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
            if (option != JOptionPane.YES_NO_OPTION) {
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
        changePortButton.setEnabled(enabled);
        removeClientButton.setEnabled(true); // Abilitato sempre
    }

    private void updateStatus(String status) {
        statusLabel.setText("Status: " + status);
    }

    // Metodi per aggiornare la lista dei client dalla ServerConnection
    public void addClientToList(String clientName) {
        SwingUtilities.invokeLater(() -> clientsListModel.addElement(clientName));
    }

    public void removeClientFromList(String clientName) {
        SwingUtilities.invokeLater(() -> clientsListModel.removeElement(clientName));
    }

    private void changePort() {
        String input = JOptionPane.showInputDialog(this, "Enter new port number:", "Change Port", JOptionPane.PLAIN_MESSAGE);
        if (input != null) {
            try {
                int newPort = Integer.parseInt(input);
                if (newPort < 0 || newPort > 65535) {
                    JOptionPane.showMessageDialog(this, "Port must be between 0 and 65535.", "Invalid Port", JOptionPane.ERROR_MESSAGE);
                } else {
                    port = newPort;
                    updatePortLabel();
                    updateStatus("Port changed to " + port);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid integer.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updatePortLabel() {
        portLabel.setText("Port: " + port);
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

    public static void main(String[] args) {
        // Avvia l'interfaccia grafica del server
        SwingUtilities.invokeLater(() -> {
            ServerGUI serverGUI = new ServerGUI();
            serverGUI.setVisible(true);
        });
    }
}
