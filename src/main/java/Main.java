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

  private static boolean matchChar(char c, String pattern, int[] idx) {
    if (idx[0] >= pattern.length()) {
      return false;
    }

    switch (pattern.charAt(idx[0])) {
      case '\\':
        if (idx[0] + 1 >= pattern.length()) {
          return false;
        }
        char next = pattern.charAt(idx[0] + 1);
        idx[0] += 2;
        return switch (next) {
          case 'd' -> Character.isDigit(c);
          case 'w' -> Character.isLetterOrDigit(c) || c == '_';
          default -> c == next;
        };
      case '[':
        int start = idx[0];
        boolean negate = (idx[0] + 1 < pattern.length() && pattern.charAt(idx[0] + 1) == '^');
        if (negate) {
          idx[0]++;
        }

        while (idx[0] < pattern.length() && pattern.charAt(idx[0]) != ']') {
          idx[0]++;
        }
        if (idx[0] >= pattern.length()) {
          return false;
        }

        String charClass = pattern.substring(start + 1, idx[0]);
        boolean matched = charClass.indexOf(c) != -1;
        idx[0]++;
        return negate != matched;
      default:
        idx[0]++;
        return c == pattern.charAt(idx[0] - 1);
    }
  }

  private static boolean matchHere(String input, int inputIdx, String pattern, int patternIdx) {
    if (patternIdx >= pattern.length()) {
      return true;
    }

    if (inputIdx >= input.length()) {
      return false;
    }

    int[] idx = { patternIdx };
    if (matchChar(input.charAt(inputIdx), pattern, idx)) {
      return matchHere(input, inputIdx + 1, pattern, idx[0]);
    }

    return false;
  }

  public static boolean matchPattern(String inputLine, String pattern) {
    if (!pattern.isEmpty() && pattern.charAt(0) == '^') {
      return matchHere(inputLine, 0, pattern, 1);
    }
    for (int i = 0; i <= inputLine.length(); i++) {
      if (matchHere(inputLine, i, pattern, 0)) {
        return true;
      }
    }
    return false;
  }
}
