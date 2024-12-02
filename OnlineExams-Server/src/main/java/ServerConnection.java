import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerConnection {
    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final List<Question> questions;
    private final Map<String, Score> scores;
    private final CountDownLatch startSignal;
    private CountDownLatch finishSignal;
    private final AtomicInteger connectedClients;
    private volatile boolean gameStarted = false;
    private volatile boolean isRunning = true;
    private final Set<String> usedNames = Collections.synchronizedSet(new HashSet<>());
    private final ConcurrentMap<String, ClientHandler> clientsMap;
    private ServerGUI gui; // Riferimento alla GUI
    
    public static final String START = "START";
    public static final String END = "END";
    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";

    public ServerConnection(int port, List<Question> questions) throws IOException {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("The port must be between 0 and 65535.");
        }
        this.serverSocket = new ServerSocket(port);
        this.executor = Executors.newCachedThreadPool();
        this.questions = questions;
        this.scores = new ConcurrentHashMap<>();
        this.startSignal = new CountDownLatch(1);
        this.connectedClients = new AtomicInteger(0);
        this.clientsMap = new ConcurrentHashMap<>();
    }

    // Metodo per impostare il riferimento alla GUI
    public void setGui(ServerGUI gui) {
        this.gui = gui;
    }

    public List<String> getConnectedClientNames() {
        return new ArrayList<>(clientsMap.keySet());
    }

    public boolean removeClient(String clientName) {
        ClientHandler handler = clientsMap.get(clientName);
        if (handler != null) {
            handler.sendEndMessage(); // Invia il messaggio "END" al client
            handler.disconnect(); // Disconnette il client
            clientsMap.remove(clientName);
            if (gui != null) {
                gui.removeClientFromList(clientName);
            }
            return true;
        }
        return false;
    }

    public void startGame() {
        System.out.println("Starting the game."); // Avvio del gioco
        finishSignal = new CountDownLatch(clientsMap.size());
        gameStarted = true;
        startSignal.countDown();
        System.out.println("Start signal sent."); // Segnale di inizio inviato
    }

    public void startServer() {
        try {
            while (isRunning) {
                try {
                    Socket clientSocket = acceptConnection();
                    if (!gameStarted) {
                        connectedClients.incrementAndGet();
                        executor.execute(() -> handleClient(clientSocket));
                    } else {
                        closeConnections(clientSocket, null, null);
                    }
                } catch (SocketException e) {
                    if (!isRunning) {
                        // Interruzione normale, il server sta chiudendo
                        break;
                    }
                    // Altri errori di socket
                    System.err.println("Socket error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                // Solo se non è una chiusura volontaria
                System.err.println("Server error: " + e.getMessage());
            }
        }
    }

    public void waitForAllClientsToFinish() throws InterruptedException {
        finishSignal.await();
        // Salva la classifica su file dopo che tutti i client hanno finito
        saveLeaderboardToFile();
    }

    public List<Score> getScores() {
        List<Score> leaderboard = new ArrayList<>(scores.values());
        Collections.sort(leaderboard);
        return leaderboard;
    }

    private void handleClient(Socket clientSocket) {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            String clientName = performHandshake(in, out);
            if (clientName == null) {
                System.out.println("Handshake failed with client: " + clientSocket.getInetAddress());
                return;
            }

            // Aggiungi il client alla mappa e alla GUI
            ClientHandler clientHandler = new ClientHandler(clientName, clientSocket, this);
            clientsMap.put(clientName, clientHandler);
            if (gui != null) {
                gui.addClientToList(clientName);
            }

            try {
                startSignal.await();
                
                Score playerScore = new Score(clientName, questions.size());
                scores.put(clientName, playerScore);
                
                sendQuestions(in, out, playerScore);
                
            } catch (InterruptedException e) {
                System.out.println("Client " + clientName + " interrupted");
            } finally {
                finishSignal.countDown();
                if (gameStarted) {
                    usedNames.remove(clientName);
                    gui.updateLeaderboard(getScores());
                }
                clientsMap.remove(clientName);
                if (gui != null) {
                    gui.removeClientFromList(clientName);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected unexpectedly");
            connectedClients.decrementAndGet();
        } finally {
            closeConnections(clientSocket, in, out);
        }
    }

    private String generateUniqueName(String baseName) {
        String uniqueName = baseName;
        int counter = 1;
        
        while (usedNames.contains(uniqueName)) {
            uniqueName = baseName + "_" + counter++;
        }
        
        usedNames.add(uniqueName);
        return uniqueName;
    }

    private String performHandshake(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        try {
            String requestedName = (String) in.readObject();
            String uniqueName = generateUniqueName(requestedName);
            
            out.writeObject(SUCCESS);
            out.writeObject(uniqueName);  // Invia il nome univoco al client
            out.flush();
            
            System.out.println("Client registered with name: " + uniqueName);
            return uniqueName;
            
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Handshake failed");
            throw e;
        }
    }

    private void sendQuestions(ObjectInputStream in, ObjectOutputStream out, Score playerScore) 
            throws IOException, ClassNotFoundException {
        try {
            out.writeObject(START);
            out.flush();
            System.out.println("Sent START to " + playerScore.getPlayerName());

            for (int i = 0; i < questions.size(); i++) {
                Question question = questions.get(i);
                out.writeObject(question);
                out.flush();
                System.out.println("Question sent to " + playerScore.getPlayerName() + ": " + question.getText());

                String response = (String) in.readObject();
                System.out.println("Client " + playerScore.getPlayerName() + " responded: " + response);

                // Converti la risposta in indice
                int responseIndex = convertResponseToIndex(response, question.getOptions());
                if (responseIndex == question.getCorrectAnswer()) {
                    playerScore.incrementScore();
                    System.out.println("Correct answer from " + playerScore.getPlayerName());
                } else {
                    System.out.println("Incorrect answer from " + playerScore.getPlayerName() + 
                                     " (expected: " + question.getCorrectAnswer() + 
                                     ", got: " + responseIndex + ")");
                }
            }

            out.writeObject(END);
            out.flush();
            System.out.println("Sent END to " + playerScore.getPlayerName());
            
        } catch (IOException e) {
            System.out.println("Client " + playerScore.getPlayerName() + " disconnected during quiz");
            throw e;
        }
    }

    private int convertResponseToIndex(String response, List<String> options) {
        // Cerca la risposta nell'elenco delle opzioni
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equals(response)) {
                return i;
            }
        }
        return -1; // Risposta non trovata
    }

    private synchronized void printLeaderboard() {
        List<Score> leaderboard = new ArrayList<>(scores.values());
        Collections.sort(leaderboard);

        System.out.println("\n=== FINAL LEADERBOARD ===");
        for (int i = 0; i < leaderboard.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, leaderboard.get(i));
        }
        System.out.println("=====================\n");
    }

    private Socket acceptConnection() throws IOException {
        try {
            return serverSocket.accept();
        } catch (IOException e) {
            if (!isRunning) {
                throw new SocketException("Server is shutting down");
            }
            throw new IOException("Error accepting connection: " + e.getMessage());
        }
    }

    private void closeConnections(Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connections: " + e.getMessage());
        }
    }

    public void close() throws IOException {
        System.out.println("Closing the server.");
        isRunning = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        serverSocket.close();
    }

    private void saveLeaderboardToFile() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String filename = "exam_results_" + now.format(formatter) + ".txt";
        
        String filePath = FileUtils.getLeaderboardPath(filename);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            List<Score> leaderboard = new ArrayList<>(scores.values());
            Collections.sort(leaderboard);
            
            // Informazioni generali dell'esame
            writer.println("EXAM REPORT");
            writer.println("Date: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            writer.println("Time: " + now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            writer.println("Total Questions: " + questions.size());
            writer.println("Number of Participants: " + leaderboard.size());
            
            // Statistiche generali
            double avgScore = leaderboard.stream()
                    .mapToDouble(Score::getPercentage)
                    .average()
                    .orElse(0.0);
            double maxScore = leaderboard.stream()
                    .mapToDouble(Score::getPercentage)
                    .max()
                    .orElse(0.0);
            double minScore = leaderboard.stream()
                    .mapToDouble(Score::getPercentage)
                    .min()
                    .orElse(0.0);
            
            writer.println("\nSTATISTICS");
            writer.printf("Average Score: %.1f%%%n", avgScore);
            writer.printf("Highest Score: %.1f%%%n", maxScore);
            writer.printf("Lowest Score: %.1f%%%n", minScore);
            
            // Distribuzione dei voti
            writer.println("\nGRADE DISTRIBUTION");
            int excellent = countScoresInRange(leaderboard, 90, 100);
            int good = countScoresInRange(leaderboard, 70, 89);
            int average = countScoresInRange(leaderboard, 60, 69);
            int poor = countScoresInRange(leaderboard, 0, 59);
            
            writer.printf("90-100%%: %d students%n", excellent);
            writer.printf("70-89%%:  %d students%n", good);
            writer.printf("60-69%%:  %d students%n", average);
            writer.printf("0-59%%:   %d students%n", poor);
            
            // Risultati dettagliati
            writer.println("\nDETAILED RESULTS");
            writer.printf("%-5s %-20s %-12s %-10s %-10s%n", 
                         "Rank", "Name", "Score", "Correct", "Percentage");
            writer.println("-".repeat(60));
            
            for (int i = 0; i < leaderboard.size(); i++) {
                Score score = leaderboard.get(i);
                writer.printf("%-5d %-20s %-12s %-10d %.1f%%%n",
                             i + 1,
                             score.getPlayerName(),
                             score.getCorrectAnswers() + "/" + score.getTotalQuestions(),
                             score.getCorrectAnswers(),
                             score.getPercentage());
            }
            
            System.out.println("Detailed results saved to file: " + filePath);
            
        } catch (IOException e) {
            System.err.println("Error saving results to file: " + e.getMessage());
        }
    }

    private int countScoresInRange(List<Score> scores, double min, double max) {
        return (int) scores.stream()
                .filter(s -> s.getPercentage() >= min && s.getPercentage() <= max)
                .count();
    }
}