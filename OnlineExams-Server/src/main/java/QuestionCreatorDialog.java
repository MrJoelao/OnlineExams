import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QuestionCreatorDialog extends JDialog {
    private List<Question> questions = new ArrayList<>();
    private final JTextArea questionArea;
    private final JPanel optionsPanel;
    private final JSpinner optionsCountSpinner;
    private final List<JTextField> optionFields = new ArrayList<>();
    private final JSpinner correctAnswerSpinner;
    
    public QuestionCreatorDialog(JFrame parent) {
        super(parent, "Create Questions", true);
        setLayout(new BorderLayout(10, 10));
        
        // Panel principale con padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Area domanda
        JLabel questionLabel = new JLabel("Question text:");
        questionArea = new JTextArea(3, 40);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        JScrollPane questionScroll = new JScrollPane(questionArea);
        
        // Numero di opzioni
        JPanel optionsCountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsCountPanel.add(new JLabel("Number of options:"));
        optionsCountSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 10, 1));
        optionsCountSpinner.addChangeListener(e -> updateOptionsPanel());
        optionsCountPanel.add(optionsCountSpinner);
        
        // Panel per le opzioni
        optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        JScrollPane optionsScroll = new JScrollPane(optionsPanel);
        optionsScroll.setPreferredSize(new Dimension(400, 200));
        
        // Risposta corretta
        JPanel correctAnswerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        correctAnswerPanel.add(new JLabel("Correct answer:"));
        correctAnswerSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1, 1));
        correctAnswerPanel.add(correctAnswerSpinner);
        
        // Pulsanti
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add Question");
        JButton saveButton = new JButton("Save & Close");
        buttonPanel.add(addButton);
        buttonPanel.add(saveButton);
        
        // Aggiungi componenti al panel principale
        mainPanel.add(questionLabel);
        mainPanel.add(questionScroll);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(optionsCountPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(optionsScroll);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(correctAnswerPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(buttonPanel);
        
        add(mainPanel);
        
        // Inizializza il panel delle opzioni
        updateOptionsPanel();
        
        // Listeners
        addButton.addActionListener(e -> addQuestion());
        saveButton.addActionListener(e -> {
            if (!questions.isEmpty()) {
                saveQuestions();
            }
            dispose();
        });
        
        // Gestione chiusura finestra
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!questions.isEmpty()) {
                    int option = JOptionPane.showConfirmDialog(
                        QuestionCreatorDialog.this,
                        "Do you want to save the questions before closing?",
                        "Save Questions",
                        JOptionPane.YES_NO_CANCEL_OPTION
                    );
                    if (option == JOptionPane.YES_OPTION) {
                        saveQuestions();
                    } else if (option == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                }
                dispose();
            }
        });
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void updateOptionsPanel() {
        optionsPanel.removeAll();
        optionFields.clear();
        
        int count = (Integer) optionsCountSpinner.getValue();
        correctAnswerSpinner.setModel(new SpinnerNumberModel(0, 0, count - 1, 1));
        
        for (int i = 0; i < count; i++) {
            JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            optionPanel.add(new JLabel("Option " + i + ":"));
            JTextField optionField = new JTextField(30);
            optionFields.add(optionField);
            optionPanel.add(optionField);
            optionsPanel.add(optionPanel);
        }
        
        optionsPanel.revalidate();
        optionsPanel.repaint();
    }
    
    private void addQuestion() {
        String questionText = questionArea.getText().trim();
        if (questionText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a question text", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        List<String> options = new ArrayList<>();
        for (JTextField field : optionFields) {
            String option = field.getText().trim();
            if (option.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All options must be filled", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            options.add(option);
        }
        
        int correctAnswer = (Integer) correctAnswerSpinner.getValue();
        questions.add(new Question(questionText, options, correctAnswer));
        
        // Clear fields
        questionArea.setText("");
        for (JTextField field : optionFields) {
            field.setText("");
        }
        
        JOptionPane.showMessageDialog(this, 
            "Question added! Total questions: " + questions.size(), 
            "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void saveQuestions() {
        if (questions.isEmpty()) return;
        
        String defaultName = "quiz_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
        String fileName = JOptionPane.showInputDialog(this, "Enter file name:", defaultName);
        
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = defaultName;
        }
        
        String filePath = FileUtils.getDefaultPath(fileName);
        
        try {
            QuestionWriter.writeQuestionsToFile(filePath, questions);
            JOptionPane.showMessageDialog(this,
                "Questions saved to: " + filePath,
                "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Error saving questions: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public List<Question> getQuestions() {
        return questions.isEmpty() ? null : new ArrayList<>(questions);
    }
} 