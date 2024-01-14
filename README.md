# We are all to gather
Demo of the new Stream Gatherer API of Java 22

A simple way to discover and understand the Stream Gatherer API is to re-implement some of the already existing stream
intermediary operations like (map, filter, takeWhile, etc.) using the new Gatherer API.

A Gatherer is composed of 4 functions
- an initializer () -> A, initializes a state (if there is one)
- an integrator (state, element, downstream) -> boolean,
  updates the state and/or push transformed elements to the next stage and
  back-propagate the stop boolean
- a combiner (state, state) -> state, merge two states (if the computation is done in parallel)
- a finisher (state, downstream), push transformed elements to the next stage (if necessary)

A Gatherer is created by answering 3 questions
- Is the operation paralellizable or sequential ?
  if paralelizable, use Gatherer.of() + a combiner? or Gatherer.ofSequential() to create the Gatherer.
- Is the operation stateful or stateless ?
  if stateful, call of()/ofSequential() with 2 or 3 parameters (initializer, integrator, finisher?).
- Is the operation greedy or short-circuit ?
- if greedy, use Integrator.ofGreedy(integrator) or just the integrator.

### map

Let's try to implement `stream.map(mapper)`, which transform any elements to another elements,
`stream.map()` is paralellizable, stateless and greedy.

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

A Gatherer is parameterized by 3 type arguments, the type of the element (here `String`),
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

Let's now try to implement `stream.filter(predicate)` which keep the element that have the predicate function that returns true,
`stream.filter()` is parallelizable, stateless and greedy.

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

For `stream.takeWhile(predicate)`, the elements are kept while the predicate is true,
`stream.takeWhile()` is sequential, stateless and short-circuit.

```java
  var result = text.lines()
      //.takeWhile(s -> s.startsWith("item"))
      .gather(takeWhile())
      .toList();   // [item1, item2]
```

The gatherer is created by `Gatherer.ofSequential(integrator)`. Inside the `integrator`,
if the predicate is true for the current element, the element is pushed to the next stage,
otherwise, the operation short-circuit by returning `false`.

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

For `stream.limit(int)`, we need to count the number of elements seen, so we need a state for that,
`stream.limit()` is sequential, stateful and short-circuit.

```java
  var result = text.lines()
      //.limit(3)
      .gather(limit())
      .toList();
```

The Gatherer is created with `ofSequential(initializer, integrator)`. The initializer create the state,
the integrator modify the state counter until the limit and return `false`.

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

The gatherer API also comes with few gatherers defined in the class `Gatherers`.
For example, if we want to group all the elements by 2 (in a List), there is already
`Gatheres.windowFixed(2)` for that. `Gatheres.windowFixed()` is sequential, stateful and greedy.


```java
  var result = text.lines()
      //.gather(Gatherers.windowFixed(2))
      .gather(windowFixed())
      .toList();
```

Let's re-implement `windowFixed`. The gatherer is created with `Gatherer.ofSequential()` and takes
an initializer to initialize the state, a greedy integrator and also a finisher. The finisher is needed here
because if the number of elements is not a multiple of 2, we need to emit a List with one element at the end.
The state is a list that will contain the element until its size is 2. At that point, the list is pushed to
the downstream stage and the state uses a new list. If the downstream stage stop the computation,
we need to back-propage `false`. In the finisher, if the state list as element in it, the list is pushed to
the downstream stage.

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
        if (!list.isEmpty()) {
          downstream.push(state.list);
        }
      }
  );
}
```


### fold vs reduce

`Gatherers.fold()` is another builtin gatherer. It accumulates the value from left to right and
unlike `stream.reduce()` it does not require the operation to be associate.
`stream.reduce()` is parallelizable, so it may split the computation in several parts
(to run on different cores) thus requires the operation to be associative to merge the result of the different parts.
`fold()` is sequential, stateful and greedy. 

```java
  var result = text.lines()
      //.gather(Gatherers.fold(() -> 0, (value, s) -> value + 1))
      .gather(fold())
      .findFirst().orElseThrow();
```

Let's rewrite `fold`. The gatherer is created using `ofSequential` with an initializer, a greedy integrator and
a finisher. The integrator does not push the value to the downstream stage and only accumulate the values.
At the end, the finisher push the result (here the `state.counter`).

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

Instead of `fold`, we may want to re-implement `reduce`, which is paralellizable, stateful and greedy.
The gatherer is created with `Gatherer.of()` with an initializer, a greedy integrator, a combiner and
a finisher. Because the combiner need to re-create a State from two existing state, the state has to be named
(it is created more than once), that's the prupose of the local class `State`.

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

We can test that `reduce` works correctly by asking for a parallel stream.

```java
    var result = text.lines()
      .parallel()
      .gather(reduce())
      .findFirst().orElseThrow();
```

### Collector as Gatherer

We have seeing with `reduce()` that a `Collector` can be written as a `Gatherer`.

```java
  var list = List.of(1, 2, 3, 4, 5);
  var result = list.stream()
      .gather(asGatherer(Collectors.toList()))
      .findFirst().orElseThrow();
  System.out.println(result);
```

A collector is a gatherer which is parallelizable, stateful and greedy.
So such gatherer should be created with `Gatherer.of()` with an initializer, a greedy integrator,
a combiner and a finisher. Like with `fold` or `reduce`, the finisher will send the result
to the downstream stage.

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

Okay, that's all for today. I hope you enjoy it as well as me.
