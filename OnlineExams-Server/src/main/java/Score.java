import java.io.Serializable;

public class Score implements Comparable<Score>, Serializable {
    private final String playerName;
    private int correctAnswers;
    private final int totalQuestions;

    public Score(String playerName, int totalQuestions) {
        this.playerName = playerName;
        this.correctAnswers = 0;
        this.totalQuestions = totalQuestions;
    }

    public void incrementScore() {
        correctAnswers++;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public double getPercentage() {
        return (correctAnswers * 100.0) / totalQuestions;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    @Override
    public int compareTo(Score other) {
        int scoreCompare = Integer.compare(other.correctAnswers, this.correctAnswers);
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        return this.playerName.compareTo(other.playerName);
    }

    @Override
    public String toString() {
        return String.format("%s: %d/%d (%.1f%%)", 
            playerName, correctAnswers, totalQuestions, getPercentage());
    }
} 