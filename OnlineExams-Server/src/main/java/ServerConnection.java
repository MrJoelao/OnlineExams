import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerConnection {
    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final List<Question> questions;
    private final Map<String, Score> scores;
    private final CountDownLatch startSignal;
    private CountDownLatch finishSignal;
    private final AtomicInteger connectedClients;
    private volatile boolean gameStarted = false;
    private final Set<String> usedNames = Collections.synchronizedSet(new HashSet<>());
    
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
    }

    public void startGame() {
        finishSignal = new CountDownLatch(connectedClients.get());
        gameStarted = true;
        startSignal.countDown();
    }

    public void startServer() {
        try {
            while (!executor.isShutdown()) {
                Socket clientSocket = acceptConnection();
                if (!gameStarted) {
                    connectedClients.incrementAndGet();
                    executor.execute(() -> handleClient(clientSocket));
                } else {
                    // Se il gioco è già iniziato, rifiuta nuove connessioni
                    closeConnections(clientSocket, null, null);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void waitForAllClientsToFinish() throws InterruptedException {
        finishSignal.await();
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
            System.out.println("Sent: START to " + playerScore.getPlayerName());

            for (Question question : questions) {
                out.writeObject(question);
                out.flush();
                System.out.println("Question sent to " + playerScore.getPlayerName() + ": " + question.getText());

                String response = (String) in.readObject();
                if (isAnswerCorrect(question, response)) {
                    playerScore.incrementScore();
                }
                System.out.println("Received response from " + playerScore.getPlayerName() + ": " + response);
            }

            out.writeObject(END);
            out.flush();
            System.out.println("Sent: END to " + playerScore.getPlayerName());
            
        } catch (IOException e) {
            System.out.println("Client " + playerScore.getPlayerName() + " disconnected during quiz");
            throw e;
        }
    }

    private boolean isAnswerCorrect(Question question, String response) {
        try {
            int answerIndex = Integer.parseInt(response);
            return answerIndex == question.getCorrectAnswer();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private synchronized void printLeaderboard() {
        List<Score> leaderboard = new ArrayList<>(scores.values());
        Collections.sort(leaderboard);

        System.out.println("\n=== CLASSIFICA FINALE ===");
        for (int i = 0; i < leaderboard.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, leaderboard.get(i));
        }
        System.out.println("=====================\n");
    }

    private Socket acceptConnection() throws IOException {
        try {
            return serverSocket.accept();
        } catch (IOException e) {
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
        try {
            executor.shutdown();
            serverSocket.close();
        } catch (IOException e) {
            throw new IOException("Error closing connection: " + e.getMessage());
        }
    }
}