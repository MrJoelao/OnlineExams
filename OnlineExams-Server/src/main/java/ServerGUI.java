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

    private JList<String> leaderboardList;
    private DefaultListModel<String> leaderboardListModel;

    public ServerGUI() {
        setLookAndFeel();
        setupFrame();
        setupLogging();
        setupListeners();
        initializeClientsList(); // Inizializza la lista dei client
        initializeLeaderboardList(); // Inizializza la lista della classifica
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

    private void initializeLeaderboardList() {
        leaderboardListModel = new DefaultListModel<>();
        leaderboardList.setModel(leaderboardListModel);
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
        System.out.println("Inizio caricamento delle domande."); // Avvio del caricamento delle domande
        JFileChooser fileChooser = new JFileChooser(FileUtils.getDefaultPath(""));
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = fileChooser.getSelectedFile();
                loadedQuestions = QuestionParser.parseQuestionsFile(selectedFile.getPath());
                System.out.println("Domande caricate con successo: " + loadedQuestions.size()); // Domande caricate correttamente
                updateStatus("Loaded " + loadedQuestions.size() + " questions");
                startQuizButton.setEnabled(true);
            } catch (IOException ex) {
                System.out.println("Errore durante il caricamento delle domande: " + ex.getMessage()); // Errore nel caricamento delle domande
                JOptionPane.showMessageDialog(this, 
                    "Error loading questions: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.out.println("Caricamento delle domande annullato."); // Annullamento del caricamento delle domande
        }
    }

    private void createQuestions() {
        System.out.println("Inizio creazione di nuove domande."); // Avvio della creazione delle domande
        QuestionCreatorDialog dialog = new QuestionCreatorDialog(this);
        dialog.setVisible(true);
        
        if (dialog.getQuestions() != null) {
            loadedQuestions = dialog.getQuestions();
            System.out.println("Domande create con successo: " + loadedQuestions.size()); // Domande create correttamente
            updateStatus("Created " + loadedQuestions.size() + " questions");
            startQuizButton.setEnabled(true);
        } else {
            System.out.println("Creazione delle domande annullata."); // Annullamento della creazione delle domande
        }
    }

    private void startQuiz() {
        if (loadedQuestions == null || loadedQuestions.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please load questions first", 
                "Error", JOptionPane.ERROR_MESSAGE);
            System.out.println("Tentativo di avviare il quiz senza domande caricate."); // Tentativo di avvio senza domande
            return;
        }

        try {
            serverConnection = new ServerConnection(port, loadedQuestions);
            serverConnection.setGui(this); // Imposta il riferimento alla GUI per aggiornamenti
            // Disabilita i pulsanti durante il quiz, eccetto removeClientButton
            setButtonsEnabled(false);
            quizRunning = true;
            updateStatus("Server started on port " + port);
            System.out.println("Server avviato sulla porta " + port); // Server avviato

            // Abilita solo removeClientButton
            removeClientButton.setEnabled(true);

            // Avvia il server in background
            new Thread(() -> {
                try {
                    serverConnection.startServer();
                    System.out.println("Server in ascolto per connessioni..."); // Server in ascolto
                } catch (Exception ex) {
                    if (quizRunning) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, 
                                "Server error: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                            System.out.println("Errore del server: " + ex.getMessage()); // Errore del server
                        });
                    }
                }
            }).start();

            // Attendi i client
            int option = JOptionPane.showConfirmDialog(this,
                "Start the quiz?",
                "Start Quiz", JOptionPane.YES_NO_OPTION);
            
            if (option == JOptionPane.YES_OPTION) {
                System.out.println("Quiz avviato dall'utente."); // Quiz avviato dall'utente
                serverConnection.startGame();
                updateStatus("Quiz in progress...");
                
                // Attendi il completamento del quiz in background
                new Thread(() -> {
                    try {
                        serverConnection.waitForAllClientsToFinish();
                        System.out.println("Quiz terminato, visualizzazione dei risultati."); // Quiz terminato
                        displayResults();
                        cleanup();
                        refreshClientsList();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            } else {
                System.out.println("Quiz non avviato dall'utente."); // Quiz non avviato
                cleanup();
                refreshClientsList();
            }
            
        } catch (IOException ex) {
            System.out.println("Errore nell'avvio del server: " + ex.getMessage()); // Errore nell'avvio del server
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
            System.out.println("Tentativo di rimuovere un client senza selezione."); // Tentativo senza selezione
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to remove client: " + selectedClient + "?",
            "Confirm Removal", JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            System.out.println("Rimozione del client: " + selectedClient); // Rimozione del client
            boolean success = serverConnection.removeClient(selectedClient);
            if (success) {
                JOptionPane.showMessageDialog(this, "Client " + selectedClient + " has been removed.", "Client Removed", JOptionPane.INFORMATION_MESSAGE);
                System.out.println("Client " + selectedClient + " rimosso con successo."); // Client rimosso
                refreshClientsList();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to remove client: " + selectedClient, "Error", JOptionPane.ERROR_MESSAGE);
                System.out.println("Fallita la rimozione del client: " + selectedClient); // Fallimento rimozione
            }
        } else {
            System.out.println("Rimozione del client " + selectedClient + " annullata dall'utente."); // Annullamento rimozione
        }
    }

    private void displayResults() {
        SwingUtilities.invokeLater(() -> {
            List<Score> leaderboard = serverConnection.getScores();
            updateLeaderboard(leaderboard);
            StringBuilder results = new StringBuilder("\n=== FINAL RANKINGS ===\n");
            for (int i = 0; i < leaderboard.size(); i++) {
                results.append(String.format("%d. %s%n", i + 1, leaderboard.get(i)));
            }
            results.append("=====================\n");
            logArea.append(results.toString());

            // Mostra un pop-up con il primo in classifica
            if (!leaderboard.isEmpty()) {
                String topScorer = leaderboard.get(0).toString();
                JOptionPane.showMessageDialog(this, "The winner is: " + topScorer, "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
            }
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

    public void updateLeaderboard(List<Score> leaderboard) {
        SwingUtilities.invokeLater(() -> {
            leaderboardListModel.clear();
            for (Score score : leaderboard) {
                leaderboardListModel.addElement(score.toString());
            }
        });
    }

    public static void main(String[] args) {
        // Avvia l'interfaccia grafica del server
        SwingUtilities.invokeLater(() -> {
            ServerGUI serverGUI = new ServerGUI();
            serverGUI.setVisible(true);
        });
    }
}
