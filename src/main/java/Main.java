import java.util.*;

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

  private static int findMatchingParen(String pattern, int openParenPos) {
    int depth = 1;
    for (int i = openParenPos + 1; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c == '(')
        depth++;
      else if (c == ')') {
        depth--;
        if (depth == 0)
          return i;
      }
    }
    return -1;
  }

  private static boolean matchChar(char c, String pattern, int[] idx) {
    if (idx[0] >= pattern.length()) {
      return false;
    }

    switch (pattern.charAt(idx[0])) {
      case '\\':
        if (idx[0] + 1 >= pattern.length())
          return false;
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
        if (negate)
          idx[0]++;
        while (idx[0] < pattern.length() && pattern.charAt(idx[0]) != ']') {
          idx[0]++;
        }
        if (idx[0] >= pattern.length())
          return false;
        String charClass = pattern.substring(start + 1, idx[0]);
        boolean matched = charClass.indexOf(c) != -1;
        idx[0]++;
        return negate != matched;
      case '.':
        idx[0]++;
        return c != '\n';
      default:
        idx[0]++;
        return c == pattern.charAt(idx[0] - 1);
    }
  }

  private static List<String> splitTopLevelAlternatives(String s) {
    List<String> alts = new ArrayList<>();
    int depth = 0;
    StringBuilder cur = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      if (ch == '(')
        depth++;
      else if (ch == ')')
        depth--;
      if (ch == '|' && depth == 0) {
        alts.add(cur.toString());
        cur.setLength(0);
      } else {
        cur.append(ch);
      }
    }
    alts.add(cur.toString());
    return alts;
  }

  private static boolean matchHere(String input, int inputIdx, String pattern, int patternIdx) {
    if (patternIdx >= pattern.length()) {
      return true;
    }

    if (pattern.charAt(patternIdx) == '^') {
      return inputIdx == 0 && matchHere(input, inputIdx, pattern, patternIdx + 1);
    }

    if (pattern.charAt(patternIdx) == '$') {
      return inputIdx == input.length() && patternIdx + 1 == pattern.length();
    }

    if (pattern.charAt(patternIdx) == '(') {
      int closeParen = findMatchingParen(pattern, patternIdx);
      if (closeParen == -1)
        return false;
      String group = pattern.substring(patternIdx + 1, closeParen);
      String after = (closeParen + 1 < pattern.length()) ? pattern.substring(closeParen + 1) : "";

      if (closeParen + 1 < pattern.length()) {
        char q = pattern.charAt(closeParen + 1);
        if (q == '*' || q == '+' || q == '?') {
          String rest = pattern.substring(closeParen + 2);
          return matchGroupWithQuantifier(input, inputIdx, group, rest, q);
        }
      }

      return tryGroupOnceThenRest(input, inputIdx, group, after);
    }

    if (inputIdx >= input.length()) {
      return false;
    }

    if (patternIdx < pattern.length() - 1) {
      char nextChar = pattern.charAt(patternIdx + 1);

      if (nextChar == '*') {
        int[] idx = { patternIdx };
        if (!matchChar(input.charAt(inputIdx), pattern, idx)) {
          return matchHere(input, inputIdx, pattern, patternIdx + 2);
        }
        int cur = inputIdx;
        while (cur < input.length()) {
          int[] tmp = { patternIdx };
          if (!matchChar(input.charAt(cur), pattern, tmp))
            break;
          cur++;
          if (matchHere(input, cur, pattern, patternIdx + 2))
            return true;
        }
        return matchHere(input, inputIdx, pattern, patternIdx + 2);
      }

      if (nextChar == '+') {
        int[] idx = { patternIdx };
        if (!matchChar(input.charAt(inputIdx), pattern, idx)) {
          return false;
        }
        int cur = inputIdx + 1;
        while (true) {
          if (matchHere(input, cur, pattern, idx[0] + 1))
            return true;
          if (cur >= input.length())
            break;
          int[] tmp = { patternIdx };
          if (!matchChar(input.charAt(cur), pattern, tmp))
            break;
          cur++;
        }
        return false;
      }

      if (nextChar == '?') {
        int[] idx = { patternIdx };
        if (matchChar(input.charAt(inputIdx), pattern, idx)) {
          if (matchHere(input, inputIdx + 1, pattern, patternIdx + 2)) {
            return true;
          }
        }
        return matchHere(input, inputIdx, pattern, patternIdx + 2);
      }
    }

    int[] idx = { patternIdx };
    if (matchChar(input.charAt(inputIdx), pattern, idx)) {
      return matchHere(input, inputIdx + 1, pattern, idx[0]);
    }

    return false;
  }

  private static boolean tryGroupOnceThenRest(String input, int inputIdx, String group, String afterGroup) {
    List<String> alts = splitTopLevelAlternatives(group);
    for (String alt : alts) {
      String newPattern = alt + afterGroup;
      if (matchHere(input, inputIdx, newPattern, 0))
        return true;
    }
    return false;
  }

  private static boolean matchGroupWithQuantifier(String input, int inputIdx, String group, String afterGroup, char q) {
    if (q == '?') {
      if (tryGroupOnceThenRest(input, inputIdx, group, afterGroup))
        return true;
      return matchHere(input, inputIdx, afterGroup, 0);
    }

    int maxRepeatsBound = input.length() - inputIdx + 1;
    if (q == '*') {
      StringBuilder rep = new StringBuilder();
      for (int r = 0; r <= maxRepeatsBound; r++) {
        String newPattern = rep.toString() + afterGroup;
        if (matchHere(input, inputIdx, newPattern, 0))
          return true;
        rep.append('(').append(group).append(')');
      }
      return false;
    }

    if (q == '+') {
      StringBuilder rep = new StringBuilder();
      rep.append('(').append(group).append(')');
      for (int r = 1; r <= maxRepeatsBound; r++) {
        String newPattern = rep.toString() + afterGroup;
        if (matchHere(input, inputIdx, newPattern, 0))
          return true;
        rep.append('(').append(group).append(')');
      }
      return false;
    }

    return false;
  }

  public static boolean matchPattern(String inputLine, String pattern) {
    if (!pattern.isEmpty() && pattern.charAt(0) == '^') {
      return matchHere(inputLine, 0, pattern, 0);
    }
    for (int i = 0; i <= inputLine.length(); i++) {
      if (matchHere(inputLine, i, pattern, 0))
        return true;
    }
    return false;
  }
}
