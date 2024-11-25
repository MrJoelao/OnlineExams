import javax.swing.*;
import java.awt.CardLayout;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class ClientGUI extends JFrame {
    private JPanel mainPanel;
    private JPanel loginPanel;
    private JPanel examPanel;
    private JTextField nameField;
    private JButton startButton;
    private JTextArea logArea;
    private JButton connectButton;
    private JButton exitButton;
    private JPanel answerPanel;
    private ClientConnection clientConnection;
    private String name;
    private String host = "localhost";
    private int port = 12345;
    
    private static final String LOGIN_CARD = "loginCard";
    private static final String EXAM_CARD = "examCard";

    public ClientGUI() {
        setLookAndFeel("Darcula");
        setupFrame();
        setupMenu();
        setupListeners();
        showLoginPanel();
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void setupFrame() {
        setTitle("Online Exams Client");
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("☰"); // Simula un menu ad hamburger

        JMenuItem themeItem = new JMenuItem("Choose Theme");
        themeItem.addActionListener(e -> chooseTheme());
        menu.add(themeItem);

        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(e -> showSettingsDialog());
        menu.add(settingsItem);

        menuBar.add(menu);
        setJMenuBar(menuBar);
    }

    private void setupListeners() {
        startButton.addActionListener(e -> handleStartButton());
        connectButton.addActionListener(e -> connectToServer());
        exitButton.addActionListener(e -> exitApplication());
        
        nameField.addActionListener(e -> handleStartButton());
    }

    private void showLoginPanel() {
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, LOGIN_CARD);
    }

    private void showExamPanel() {
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, EXAM_CARD);
    }

    private void handleStartButton() {
        String input = nameField.getText().trim();
        if (input.isEmpty()) {
            showErrorDialog("Invalid Name", "Name cannot be empty. Please try again.");
            return;
        }
        
        name = input;
        showExamPanel();
        logArea.append("Welcome, " + name + "!\n");
    }

    private void connectToServer() {
        if (name == null || name.trim().isEmpty()) {
            showErrorDialog("Invalid Name", "Please enter your name first.");
            showLoginPanel();
            return;
        }
        
        clientConnection = new ClientConnection(host, port, name);
        try {
            clientConnection.autoConnectForced();
            logArea.append("Connected to server as: " + clientConnection.handshake() + "\n");
            listenForQuestions();
        } catch (IOException | ClassNotFoundException e) {
            logArea.append("Error connecting to server: " + e.getMessage() + "\n");
        }
    }

    private void listenForQuestions() {
        new Thread(() -> {
            try {
                while (true) {
                    Object receivedObject = clientConnection.read();
                    if (clientConnection.isCommunicationEnded()) {
                        SwingUtilities.invokeLater(() -> {
                            logArea.append("Quiz completed!\n");
                            showExamEndDialog();
                        });
                        break;
                    }
                    if (clientConnection.isQuestion(receivedObject)) {
                        Question question = clientConnection.getQuestion();
                        displayQuestion(question);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append("Error during quiz: " + e.getMessage() + "\n");
                    showErrorDialog("Connection Error", "Lost connection to server: " + e.getMessage());
                });
            }
        }).start();
    }

    private void displayQuestion(Question question) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("\nQuestion: " + question.getText() + "\n");
            answerPanel.removeAll();
            answerPanel.setLayout(new BoxLayout(answerPanel, BoxLayout.Y_AXIS));
            
            List<String> options = question.getOptions();
            for (String option : options) {
                JButton optionButton = new JButton(option);
                optionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                optionButton.setFont(new Font(optionButton.getFont().getName(), Font.PLAIN, 14));
                optionButton.setMargin(new Insets(10, 10, 10, 10));
                optionButton.addActionListener(e -> sendAnswer(option));
                answerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                answerPanel.add(optionButton);
            }
            answerPanel.revalidate();
            answerPanel.repaint();
        });
    }

    private void sendAnswer(String answer) {
        try {
            clientConnection.send(answer);
            logArea.append("Answer sent: " + answer + "\n");
        } catch (IOException e) {
            logArea.append("Error sending answer: " + e.getMessage() + "\n");
        }
    }

    private void exitApplication() {
        if (clientConnection != null) {
            clientConnection.close();
        }
        dispose();
        System.exit(0);
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
        } catch (Exception e) {
            System.err.println("Error setting the theme: " + e.getMessage());
        }
    }

    private void chooseTheme() {
        String[] themes = {"Dark", "Light", "IntelliJ"};
        String selectedTheme = (String) JOptionPane.showInputDialog(
            this,
            "Select Theme:",
            "Theme Chooser",
            JOptionPane.PLAIN_MESSAGE,
            null,
            themes,
            themes[0]
        );

        if (selectedTheme != null) {
            try {
                setLookAndFeel(selectedTheme);
                SwingUtilities.updateComponentTreeUI(this);
                System.out.println("Theme changed to: " + selectedTheme); // Theme update
            } catch (Exception ex) {
                System.err.println("Error changing the theme: " + ex.getMessage());
            }
        } else {
            System.out.println("Theme selection cancelled by the user."); // Theme selection cancellation
        }
    }

    private void showSettingsDialog() {
        JTextField ipField = new JTextField(host);
        JTextField portField = new JTextField(String.valueOf(port));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Server IP:"));
        panel.add(ipField);
        panel.add(new JLabel("Server Port:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            host = ipField.getText().trim();
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException e) {
                logArea.append("Invalid port number.\n");
            }
        }
    }

    private void showExamEndDialog() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                this,
                "You have completed the exam!\nThank you for your participation.",
                "Exam Completed",
                JOptionPane.INFORMATION_MESSAGE
            );
            connectButton.setEnabled(false);
        });
    }

    private void showErrorDialog(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
            );
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientGUI clientGUI = new ClientGUI();
            clientGUI.setVisible(true);
        });
    }
}
