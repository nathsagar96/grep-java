import java.io.IOException;
import java.util.Scanner;

public class Main {
  public static void main(String[] args) {
    if (args.length != 2 || !args[0].equals("-E")) {
      System.out.println("Usage: ./your_program.sh -E <pattern>");
      System.exit(1);
    }

    String pattern = args[1];
    Scanner scanner = new Scanner(System.in);
    String inputLine = scanner.nextLine();

    if (matchPattern(inputLine, pattern)) {
      System.exit(0);
    } else {
      System.exit(1);
    }
  }

  public static boolean matchPattern(String inputLine, String pattern) {
    if (pattern.length() == 1) {
      return inputLine.contains(pattern);
    } else if (pattern.equals("\\d")) {
      return inputLine.chars().anyMatch(Character::isDigit);
    } else if (pattern.equals("\\w")) {
      return inputLine.chars().anyMatch(c -> Character.isLetterOrDigit(c) || c == '_');
    } else if (pattern.startsWith("[") && pattern.endsWith("]")) {
      if (pattern.startsWith("[^")) {
        String chars = pattern.substring(2, pattern.length() - 1);
        return inputLine.chars().anyMatch(c -> chars.indexOf(c) == -1);
      } else {
        String chars = pattern.substring(1, pattern.length() - 1);
        return inputLine.chars().anyMatch(c -> chars.indexOf(c) != -1);
      }
    } else {
      throw new RuntimeException("Unhandled pattern: " + pattern);
    }
  }
}
