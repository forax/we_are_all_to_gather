import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

enum Characteristic {
  SEQUENTIAL, STATELESS, GREEDY
}

List<Characteristic> characteristics(Gatherer<?,?, ?> gatherer) {
  return Stream.of(gatherer)
      .mapMulti((Gatherer<?,?,?> g, Consumer<Characteristic> consumer) -> {
        if (g.combiner() == Gatherer.defaultCombiner()) {
          consumer.accept(Characteristic.SEQUENTIAL);
        }
        if (g.initializer() == Gatherer.defaultInitializer()) {
          consumer.accept(Characteristic.STATELESS);
        }
        if (g.integrator() instanceof Gatherer.Integrator.Greedy<?,?,?>) {
          consumer.accept(Characteristic.GREEDY);
        }
      })
      .toList();
}

void main() {
  // Gatherers
  System.out.println("fold: " + characteristics(Gatherers.fold(() -> null, (_, _) -> null)));
  System.out.println("scan: " + characteristics(Gatherers.scan(() -> null, (_, _) -> null)));
  System.out.println("mapConcurrent: " + characteristics(Gatherers.mapConcurrent(10, _ -> null)));
  System.out.println("windowFixed: " + characteristics(Gatherers.windowFixed(2)));
  System.out.println("windowSliding: " + characteristics(Gatherers.windowSliding(2)));
}
