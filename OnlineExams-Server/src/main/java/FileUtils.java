import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

public class FileUtils {
    private static final String DEFAULT_DIRECTORY = "questions";
    private static final String LEADERBOARD_DIRECTORY = "leaderboards";
    
    public static String getDefaultPath(String filename) {
        createDefaultDirectory();
        return Paths.get(DEFAULT_DIRECTORY, filename).toString();
    }
    
    public static String resolvePath(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        // Se il path non include una directory, usa quella predefinita
        File file = new File(input);
        if (!file.getParentFile().exists()) {
            return getDefaultPath(file.getName());
        }
        
        return input;
    }
    
    private static void createDefaultDirectory() {
        try {
            Files.createDirectories(Paths.get(DEFAULT_DIRECTORY));
        } catch (IOException e) {
            System.err.println("Could not create default directory: " + e.getMessage());
        }
    }
    
    public static String getLeaderboardPath(String filename) {
        createLeaderboardDirectory();
        return Paths.get(LEADERBOARD_DIRECTORY, filename).toString();
    }
    
    private static void createLeaderboardDirectory() {
        try {
            Files.createDirectories(Paths.get(LEADERBOARD_DIRECTORY));
        } catch (IOException e) {
            System.err.println("Could not create leaderboard directory: " + e.getMessage());
        }
    }
} 