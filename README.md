# We are all to gather
Demo of the new Stream Gatherer API of Java 22

A simple way to discover and understand the Stream Gatherer API is to re-implement some of the already existing stream
intermediary operations like (map, filter, takeWhile, etc.) using the new Gatherer API.

### map

Let's try to implement stream.map(), which transform any elements to another elements.

```java
void main() {
  var text = """
      item1
      item2
      --
      item11
      item12
      """;

  var result = text.lines()
      .map(String::length)
      .toList();

  System.out.println(result);  // [5, 5, 2, 6, 6]
}
```

If we want to use the Gatherer API instead, we first need to use the new stream method `gather`
that takes a `Gatherer` as parameter (here the result of the method `map()`).

```java
  var result = text.lines()
      //.map(String::toUpperCase)
      .gather(map())
      .toList();
```

A Gatherer is parameterized by 3 tye arguments, the type of the element (here `String`),
the type of the internal state (let use '?' for now) and the type of the transformed elements
(here `Integer`).

To create a Gatherer, the simplest way is to use `Gatherer.of()` with an Integrator as parameter.
An integrator is a lambda that takes 3 parameters, a state (for now there is no state, so let use '_'),
an element (the element of the stream) and downstream object, an object that represents the next
stage of the stream pipeline where we can push transformed elements using the method `push`.

The method push returns a boolean, true if more elements can be sent, false otherwise.
Here we back-propagate the return value from the next stage.

```java
Gatherer<String, ?, Integer> map() {
  return Gatherer.of((_, element, downstream) -> {
    return downstream.push(element.length());
  });
}
```

Here, map() is not an operation that can short-circuit the pipeline (decide to stop the computation of the pipeline),
so we can improve a bit the performance by declaring the Integrator _greedy_ (which means non "short circuit")
using the method `Integrator.ofGreedy()`.

```java
Gatherer<String, ?, Integer> map() {
  return Gatherer.of(Integrator.ofGreedy((_, element, downstream) -> {
    return downstream.push(element.length());
  }));
}
```

### filter

Let's now try to implement `filter` which keep the element that have the predicate function that returns true

```java
  var result = text.lines()
      //.filter(s -> s.endsWith("1"))
      .gather(filter())
      .toList();    // [item1, item11]

```

Again here, we create a Gatherer with `Gatherer.of()` with a greedy integrator. Inside the integrator,
if the predicate is true for the element, the element is pushed to the downstream stage and the fact that
the computation is stopped or not is back-propagated (with `return`).
If the predicate is false, we do not push the element and returns true to ask for more element.

```java
Gatherer<String, ?, String> filter() {
  return Gatherer.of(Integrator.ofGreedy((_, element, downstream) -> {
    if (element.endsWith("1")) {
      return downstream.push(element);
    }
    return true;
  }));
}
```

### takeWhile



```java
  var result = text.lines()
      //.takeWhile(s -> s.startsWith("item"))
      .gather(takeWhile())
      .toList();   // [item1, item2]
```

```java
Gatherer<String, ?, String> takeWhile() {
  return Gatherer.ofSequential((_, element, downstream) -> {
    if (element.startsWith("item")) {
      return downstream.push(element);
    }
    return false;
  });
}
```

### limit

```java
  var result = text.lines()
      //.limit(3)
      .gather(limit())
      .toList();
```

```java
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
```

### windowFixed

```java
  var result = text.lines()
      //.gather(Gatherers.windowFixed(2))
      .gather(windowFixed())
      .toList();
```

```java
Gatherer<String, ?, List<String>> windowFixed() {
  return Gatherer.ofSequential(
      () -> new Object() { List<String> list = new ArrayList<>(); },
      Gatherer.Integrator.ofGreedy((state, element, downstream) -> {
        if (state.list.size() == 2) {
          if (!downstream.push(state.list)) {
            return false;
          }
          state.list = new ArrayList<>();
        }
        state.list.add(element);
        return true;
      }),
      (state, downstream) -> {
        downstream.push(state.list);
        state.list = null;  // maybe ?
      }
  );
}
```


### fold vs reduce

```java
  var result = text.lines()
      //.gather(Gatherers.fold(() -> 0, (value, s) -> value + 1))
      .gather(fold())
      .findFirst().orElseThrow();
```

```java
Gatherer<String, ?, Integer> fold() {
  return Gatherer.ofSequential(
      () -> new Object() { int counter; },
      Gatherer.Integrator.ofGreedy((state, _, _) -> {
        state.counter++;
        return true;
      }),
      (state, downstream) -> {
        downstream.push(state.counter);
      }
  );
}
```

```java
  var result = text.lines()
      .parallel()
      .gather(reduce())
      .findFirst().orElseThrow();
```

```java
Gatherer<String, ?, Integer> reduce() {
  class Counter {
    int counter;
    Counter(int counter) {
      this.counter = counter;
    }
  }
  return Gatherer.of(
      () -> new Counter(0),
      Gatherer.Integrator.ofGreedy((state, _, _) -> {
        state.counter++;
        return true;
      }),
      (s1, s2) -> new Counter(s1.counter + s2.counter),
      (state, downstream) -> downstream.push(state.counter)
  );
}
```

### Collector as Gatherer

```java
  var list = List.of(1, 2, 3, 4, 5);
  var result = list.stream()
      .gather(asGatherer(Collectors.toList()))
      .findFirst().orElseThrow();
  System.out.println(result);
```

```java
<E, A, T> Gatherer<E, A, T> asGatherer(Collector<? super E, A, ? extends T> collector) {
  var supplier = collector.supplier();
  var accumulator = collector.accumulator();
  var combiner = collector.combiner();
  var finisher = collector.finisher();
  return Gatherer.of(supplier,
      Gatherer.Integrator.ofGreedy((state, element, _) -> {
        accumulator.accept(state, element);
        return true;
      }),
      combiner,
      (state, downstream) -> downstream.push(finisher.apply(state)));
}
```