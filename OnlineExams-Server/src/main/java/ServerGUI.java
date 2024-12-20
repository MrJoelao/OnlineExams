import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.DefaultListModel;
import javax.swing.JList;

public class ServerGUI extends JFrame {
    private JPanel mainPanel;
    private JPanel serverPanel;
    private JTextArea logArea;
    private JButton loadQuestionsButton;
    private JButton createQuestionsButton;
    private JButton startQuizButton;
    private JLabel statusLabel;
    private JButton settingsButton;
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

    private JButton startExamButton;

    public ServerGUI() {
        setLookAndFeel("Dark");
        setupFrame();
        setupLogging();
        setupListeners();
        initializeClientsList();
        initializeLeaderboardList();
        updatePortLabel();
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void setLookAndFeel(String theme) {
        try {
            switch (theme) {
                case "Darcula":
                    UIManager.setLookAndFeel(new FlatDarculaLaf());
                    break;
                case "Dark":
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    break;
                case "Light":
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    break;
                default:
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            System.err.println("Error setting theme: " + e.getMessage());
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
        startQuizButton.addActionListener(e -> startServer());
        startExamButton.addActionListener(e -> startExam());
        settingsButton.addActionListener(e -> showSettingsDialog());
        removeClientButton.addActionListener(e -> removeSelectedClient());

        // Add window closing listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeServer();
                dispose();
            }
        });
    }

    private void closeServer() {
        if (serverConnection != null) {
            try {
                serverConnection.close();
                updateStatus("Server closed");
            } catch (IOException ex) {
                updateStatus("Error closing server: " + ex.getMessage());
            }
        }
    }

    private void loadQuestions() {
        System.out.println("Starting to load questions."); // Avvio del caricamento delle domande
        JFileChooser fileChooser = new JFileChooser(FileUtils.getDefaultPath(""));
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = fileChooser.getSelectedFile();
                loadedQuestions = QuestionParser.parseQuestionsFile(selectedFile.getPath());
                System.out.println("Questions loaded successfully: " + loadedQuestions.size()); // Domande caricate correttamente
                updateStatus("Loaded " + loadedQuestions.size() + " questions");
                startQuizButton.setEnabled(true);
            } catch (IOException ex) {
                System.out.println("Error loading questions: " + ex.getMessage()); // Errore nel caricamento delle domande
                JOptionPane.showMessageDialog(this, 
                    "Error loading questions: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.out.println("Loading questions cancelled."); // Annullamento del caricamento delle domande
        }
    }

    private void createQuestions() {
        System.out.println("Starting to create new questions."); // Avvio della creazione delle domande
        QuestionCreatorDialog dialog = new QuestionCreatorDialog(this);
        dialog.setVisible(true);
        
        if (dialog.getQuestions() != null) {
            loadedQuestions = dialog.getQuestions();
            System.out.println("Questions created successfully: " + loadedQuestions.size()); // Domande create correttamente
            updateStatus("Created " + loadedQuestions.size() + " questions");
            startQuizButton.setEnabled(true);
        } else {
            System.out.println("Creating questions cancelled."); // Annullamento della creazione delle domande
        }
    }

    private void startServer() {
        if (loadedQuestions == null || loadedQuestions.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please load questions first", 
                "Error", JOptionPane.ERROR_MESSAGE);
            System.out.println("Attempt to start the quiz without loaded questions."); // Tentativo di avvio senza domande
            return;
        }

        try {
            serverConnection = new ServerConnection(port, loadedQuestions);
            serverConnection.setGui(this); // Imposta il riferimento alla GUI per aggiornamenti
            // Disabilita i pulsanti durante il quiz, eccetto removeClientButton
            setButtonsEnabled(false);
            quizRunning = true;
            updateStatus("Server started on port " + port);
            System.out.println("Server started on port " + port); // Server avviato

            // Abilita solo removeClientButton
            removeClientButton.setEnabled(true);

            // Avvia il server in background
            new Thread(() -> {
                try {
                    serverConnection.startServer();
                    System.out.println("Server listening for connections..."); // Server in ascolto
                } catch (Exception ex) {
                    if (quizRunning) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, 
                                "Server error: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                            System.out.println("Server error: " + ex.getMessage()); // Errore del server
                        });
                    }
                }
            }).start();

            // Rimuovi il pop-up di conferma e attendi che l'utente prema il pulsante di inizio esame
            System.out.println("Server started, waiting for the user to press 'Start Exam' to begin the exam."); // Messaggio di attesa

        } catch (IOException ex) {
            System.out.println("Error starting the server: " + ex.getMessage()); // Errore nell'avvio del server
            JOptionPane.showMessageDialog(this, 
                "Error starting server: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            cleanup();
        }
    }

    private void startExam() {
        if (loadedQuestions == null || loadedQuestions.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please load questions first", 
                "Error", JOptionPane.ERROR_MESSAGE);
            System.out.println("Attempt to start the exam without loaded questions."); // Tentativo di avvio senza domande
            return;
        }

        System.out.println("Exam started by the user."); // Esame avviato dall'utente
        serverConnection.startGame();
        updateStatus("Exam in progress...");
        
        // Attendi il completamento dell'esame in background
        new Thread(() -> {
            try {
                serverConnection.waitForAllClientsToFinish();
                System.out.println("Exam finished, displaying results."); // Esame terminato
                displayResults();
                cleanup();
                refreshClientsList();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void removeSelectedClient() {
        String selectedClient = clientsList.getSelectedValue();
        if (selectedClient == null) {
            JOptionPane.showMessageDialog(this, "Please select a client to remove.", "No Client Selected", JOptionPane.WARNING_MESSAGE);
            System.out.println("Attempt to remove a client without selection."); // Tentativo senza selezione
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to remove client: " + selectedClient + "?",
            "Confirm Removal", JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            System.out.println("Removing client: " + selectedClient); // Rimozione del client
            boolean success = serverConnection.removeClient(selectedClient);
            if (success) {
                JOptionPane.showMessageDialog(this, "Client " + selectedClient + " has been removed.", "Client Removed", JOptionPane.INFORMATION_MESSAGE);
                System.out.println("Client " + selectedClient + " removed successfully."); // Client rimosso
                refreshClientsList();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to remove client: " + selectedClient, "Error", JOptionPane.ERROR_MESSAGE);
                System.out.println("Failed to remove client: " + selectedClient); // Fallimento rimozione
            }
        } else {
            System.out.println("Removal of client " + selectedClient + " cancelled by the user."); // Annullamento rimozione
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

    private void setButtonsEnabled(boolean enabled) {
        loadQuestionsButton.setEnabled(enabled);
        createQuestionsButton.setEnabled(enabled);
        startQuizButton.setEnabled(enabled);
        settingsButton.setEnabled(enabled);
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

    private void showSettingsDialog() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        
        // Port settings
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel portInputLabel = new JLabel("Port: ");
        JTextField portField = new JTextField(String.valueOf(port), 6);
        portPanel.add(portInputLabel);
        portPanel.add(portField);
        
        // Theme settings
        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel themeLabel = new JLabel("Theme: ");
        String[] themes = {"Light", "Dark", "Darcula", "System"};
        JComboBox<String> themeCombo = new JComboBox<>(themes);
        themePanel.add(themeLabel);
        themePanel.add(themeCombo);
        
        settingsPanel.add(portPanel);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(themePanel);
        
        int result = JOptionPane.showConfirmDialog(
            this,
            settingsPanel,
            "Settings",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            // Apply port changes
            try {
                int newPort = Integer.parseInt(portField.getText().trim());
                if (newPort > 0 && newPort < 65536) {
                    port = newPort;
                    updatePortLabel();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid port number. Must be between 1 and 65535.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port number format.");
            }
            
            // Apply theme changes
            String selectedTheme = (String) themeCombo.getSelectedItem();
            setLookAndFeel(selectedTheme);
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
