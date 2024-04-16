import java.util.stream.Gatherer;

Gatherer<String, ?, String> limit() {
  class Counter {
    int counter;
    Counter(int counter) { this.counter = counter; }
  }
  return Gatherer.of(
      () -> new Counter(0),
      (counter, element, downstream) -> {
        if (counter.counter++ == 3) {
          return false;
        }
        return downstream.push(element);
      },
      (c1, c2) -> new Counter(c1.counter + c2.counter),
      (_, _) -> {}

  );
}

void main() {
  var text = """
      item1
      item2
      --
      item11
      item12
      """;

  var result = text.lines()
      //.limit(3)
      .gather(limit())
      .toList();

  System.out.println(result);
}
