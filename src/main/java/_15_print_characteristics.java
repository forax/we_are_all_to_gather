enum Characteristic {
  SEQUENTIAL, STATELESS, GREEDY
}

Set<Characteristic> characteristics(Gatherer<?,?, ?> gatherer) {
  return Stream.of(gatherer)
      .<Characteristic>mapMulti((g, consumer) -> {
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
      .collect(Collectors.toCollection(() -> EnumSet.noneOf(Characteristic.class)));
}

void main() {
  // Gatherers
  System.out.println("fold: " + characteristics(Gatherers.fold(() -> null, (_, _) -> null)));
  System.out.println("scan: " + characteristics(Gatherers.scan(() -> null, (_, _) -> null)));
  System.out.println("mapConcurrent: " + characteristics(Gatherers.mapConcurrent(10, _ -> null)));
  System.out.println("windowFixed: " + characteristics(Gatherers.windowFixed(2)));
  System.out.println("windowSliding: " + characteristics(Gatherers.windowSliding(2)));
}
