import java.util.stream.Gatherer;

Gatherer<String, ?, String> limit() {
  return Gatherer.ofSequential(
      () -> new Object() { int counter; },
      (state, element, downstream) -> {
        if (state.counter++ == 3) {
          return false;
        }
        return downstream.push(element);
      }
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
