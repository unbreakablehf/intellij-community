// "Adapt using 'iterator()'" "true"
import java.util.*;
import java.util.stream.*;

class Test {
  void test() {
    Iterator<String> iterator = Stream.<caret>of("1", "2", "3");
  }
}