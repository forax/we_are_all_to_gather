package com.gihtub.forax.wearealltogather.bench;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/*
Benchmark                                           Mode  Cnt    Score    Error  Units
MapGathererBenchmark.stream_map_sum                 avgt    5  506.469 ±  0.866  us/op
MapGathererBenchmark.stream_mapToInt_sum            avgt    5  100.808 ±  0.242  us/op
MapGathererBenchmark.gatherer_map_sum               avgt    5  576.087 ±  3.119  us/op
MapGathererBenchmark.gatherer_mapSequential_sum     avgt    5  576.134 ±  1.647  us/op
MapGathererBenchmark.gatherer_mapsublcass_sum       avgt    5  616.968 ±  1.777  us/op

MapGathererBenchmark.stream_map_count               avgt    5    0.010 ±  0.001  us/op
MapGathererBenchmark.stream_mapToInt_count          avgt    5    0.010 ±  0.001  us/op
MapGathererBenchmark.gatherer_map_count             avgt    5  254.206 ±  1.578  us/op
MapGathererBenchmark.gatherer_mapsequential_count   avgt    5  251.462 ±  2.207  us/op

MapGathererBenchmark.stream_map_toList              avgt    5  321.444 ±  0.804  us/op
MapGathererBenchmark.gatherer_map_toList            avgt    5  524.822 ±  1.820  us/op
MapGathererBenchmark.gatherer_mapsequential_toList  avgt    5  537.352 ±  1.305  us/op
 */

/*
Benchmark                                                      Mode  Cnt     Score    Error  Units
MapGathererBenchmark.stream_collect                            avgt    5   225.062 ±  4.335  us/op

MapGathererBenchmark.stream_map_collect                        avgt    5   311.241 ± 62.204  us/op
MapGathererBenchmark.gatherer_map_collect                      avgt    5   307.455 ±  0.222  us/op
MapGathererBenchmark.gatherer_mapsequential_collect            avgt    5   307.612 ±  0.091  us/op
MapGathererBenchmark.gatherer_mapsublcass_collect              avgt    5   307.289 ±  0.938  us/op

MapGathererBenchmark.stream_map_collect_3                      avgt    5  1071.416 ± 82.055  us/op
MapGathererBenchmark.gatherer_map_collect_3                    avgt    5  1556.274 ±  6.962  us/op
MapGathererBenchmark.gatherer_mapsequential_collect_3          avgt    5  1549.814 ±  6.300  us/op
MapGathererBenchmark.gatherer_mapsublcass_collect_3            avgt    5  1537.387 ± 11.455  us/op

MapGathererBenchmark.gatherer_map_collect_andThen_3            avgt    5  1550.852 ±  4.271  us/op
MapGathererBenchmark.gatherer_mapsequential_collect_andThen_3  avgt    5  1549.795 ±  7.722  us/op
MapGathererBenchmark.gatherer_mapsublcass_collect_andThen_3    avgt    5  1464.286 ±  4.026  us/op
 */



@Warmup(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = { "--enable-preview", "-XX:-TieredCompilation" })
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@SuppressWarnings("static-method")
public class MapGathererBenchmark {

	private final List<Integer> integers = IntStream.range(0, 100_000).boxed().toList();
	private final List<String> strings = IntStream.range(0, 100_000).mapToObj(i -> "item" + i).toList();

	static <T, R> Gatherer<T, ?, R> map(Function<? super T, ? extends R> mapper) {
		return Gatherer.of(Gatherer.Integrator.ofGreedy((_, element, downstream) -> {
			return downstream.push(mapper.apply(element));
		}));
	}

  static <T, R> Gatherer<T, ?, R> mapSequential(Function<? super T, ? extends R> mapper) {
    return Gatherer.ofSequential(Gatherer.Integrator.ofGreedy((_, element, downstream) -> {
      return downstream.push(mapper.apply(element));
    }));
  }

	static <T, R> Gatherer<T, ?, R> mapSubclass(Function<? super T, ? extends R> mapper) {
		class MapperGatherer implements Gatherer<T, Void, R>, Gatherer.Integrator.Greedy<Void, T, R>, BinaryOperator<Void> {
			@Override public Greedy<Void, T, R> integrator() { return this; }
			@Override public BinaryOperator<Void> combiner() { return this; }

			@Override public Void apply(Void left, Void right) { return left; }
			@Override public boolean integrate(Void state, T element, Gatherer.Downstream<? super R> downstream) {
				return downstream.push(mapper.apply(element));
			}
		}
		return new MapperGatherer();
	}

  /*
	@Benchmark
	public int stream_map_sum() {
		return strings.stream().map(String::length).reduce(0, Integer::sum);
	}
	@Benchmark
	public int stream_mapToInt_sum() {
		return strings.stream().mapToInt(String::length).sum();
	}
	@Benchmark
	public int gatherer_map_sum() {
		return strings.stream().gather(map(String::length)).reduce(0, Integer::sum);
	}
  @Benchmark
  public int gatherer_mapSequential_sum() {
    return strings.stream().gather(mapSequential(String::length)).reduce(0, Integer::sum);
  }
	@Benchmark
	public int gatherer_mapsublcass_sum() {
		return strings.stream().gather(mapSubclass(String::length)).reduce(0, Integer::sum);
	}*/


	@Benchmark
	public int stream_map_collect() {
		return integers.stream().map(v -> v + 1).collect(Collectors.summingInt(v -> v));
	}
	@Benchmark
	public int gatherer_map_collect() {
		return integers.stream().gather(map(v -> v + 1)).collect(Collectors.summingInt(v -> v));
	}
  @Benchmark
  public int gatherer_mapsequential_collect() {
    return integers.stream().gather(mapSequential(v -> v + 1)).collect(Collectors.summingInt(v -> v));
  }
	@Benchmark
	public int gatherer_mapsublcass_collect() {
		return integers.stream().gather(mapSubclass(v -> v + 1)).collect(Collectors.summingInt(v -> v));
	}
	@Benchmark
	public int stream_collect() {
		return integers.stream().collect(Collectors.summingInt(v -> v + 1));
	}

	@Benchmark
	public int stream_map_collect_3() {
		return integers.stream()
				.map(v -> v + 1)
				.map(v -> v + 2)
				.map(v -> v + 3)
				.collect(Collectors.summingInt(v -> v));
	}
	@Benchmark
	public int gatherer_map_collect_3() {
		return integers.stream()
				.gather(map(v -> v + 1))
				.gather(map(v -> v + 2))
				.gather(map(v -> v + 3))
				.collect(Collectors.summingInt(v -> v));
	}
  @Benchmark
  public int gatherer_mapsequential_collect_3() {
    return integers.stream()
        .gather(mapSequential(v -> v + 1))
        .gather(mapSequential(v -> v + 2))
        .gather(mapSequential(v -> v + 3))
        .collect(Collectors.summingInt(v -> v));
  }
	@Benchmark
	public int gatherer_mapsublcass_collect_3() {
		return integers.stream()
				.gather(mapSubclass(v -> v + 1))
				.gather(mapSubclass(v -> v + 2))
				.gather(mapSubclass(v -> v + 3))
				.collect(Collectors.summingInt(v -> v));
	}
	@Benchmark
	public int gatherer_map_collect_andThen_3() {
		return integers.stream()
				.gather(map((Integer v) -> v + 1).andThen(map(v -> v + 2)).andThen(map(v -> v + 3)))
				.collect(Collectors.summingInt(v -> v));
	}
  @Benchmark
  public int gatherer_mapsequential_collect_andThen_3() {
    return integers.stream()
        .gather(mapSequential((Integer v) -> v + 1).andThen(mapSequential(v -> v + 2)).andThen(mapSequential(v -> v + 3)))
        .collect(Collectors.summingInt(v -> v));
  }
	@Benchmark
	public int gatherer_mapsublcass_collect_andThen_3() {
		return integers.stream()
				.gather(mapSubclass((Integer v) -> v + 1).andThen(mapSubclass(v -> v + 2)).andThen(mapSubclass(v -> v + 3)))
				.collect(Collectors.summingInt(v -> v));
	}

  /*
	@Benchmark
	public boolean stream_map_allMatch() {
		return strings.stream().map(s -> s.charAt(0) + 1).allMatch(v -> v < 128);
	}
	@Benchmark
	public boolean stream_mapToInt_allMatch() {
		return strings.stream().mapToInt(s -> s.charAt(0) + 1).allMatch(v -> v < 128);
	}

	private boolean map_check(int add) {
		return strings.stream().gather(map(s -> s.charAt(0) + add)).allMatch(v -> v < 128);
	}
  private boolean mapsequential_check(int add) {
    return strings.stream().gather(mapSequential(s -> s.charAt(0) + add)).allMatch(v -> v < 128);
  }
	private boolean mapSubclass_check(int add) {
		return strings.stream().gather(mapSubclass(s -> s.charAt(0) + add)).allMatch(v -> v < 128);
	}

	@Benchmark
	public boolean gatherer_map_allMatch() {
		return map_check(1);
	}
  @Benchmark
  public boolean gatherer_mapsequential_allMatch() {
    return mapSubclass_check(1);
  }
	@Benchmark
	public boolean gatherer_mapsublcass_allMatch() {
		return mapSubclass_check(1);
	}
	@Benchmark
	public boolean gatherer_map__allMatch_3() {
		return map_check(1) && map_check(2) && map_check(3);
	}
  @Benchmark
  public boolean gatherer_mapsequential__allMatch_3() {
    return mapsequential_check(1) && mapsequential_check(2) && mapsequential_check(3);
  }
	@Benchmark
	public boolean gatherer_mapsublcass__allMatch_3() {
		return mapSubclass_check(1) && mapSubclass_check(2) && mapSubclass_check(3);
	}


	@Benchmark
	public boolean map_allMatch_repeat_3() {
		return integers.stream()
				.map(v -> v + 1)
				.map(v -> v + 2)
				.map(v -> v + 3)
				.allMatch(v -> v < 1_000_000);
	}
	@Benchmark
	public boolean mapToInt_allMatch_repeat_3() {
		return integers.stream()
				.mapToInt(v -> v + 1)
				.map(v -> v + 2)
				.map(v -> v + 3)
				.allMatch(v -> v < 1_000_000);
	}
	@Benchmark
	public boolean gatherer_map_allMatch_repeat_3() {
		return integers.stream()
				.gather(map(v -> v + 1))
				.gather(map(v -> v + 2))
				.gather(map(v -> v + 3))
				.allMatch(v -> v < 1_000_000);
	}
  @Benchmark
  public boolean gatherer_mapsequential_allMatch_repeat_3() {
    return integers.stream()
        .gather(mapSequential(v -> v + 1))
        .gather(mapSequential(v -> v + 2))
        .gather(mapSequential(v -> v + 3))
        .allMatch(v -> v < 1_000_000);
  }
	@Benchmark
	public boolean gatherer_mapsublcass_allMatch_repeat_3() {
		return integers.stream()
				.gather(mapSubclass(v -> v + 1))
				.gather(mapSubclass(v -> v + 2))
				.gather(mapSubclass(v -> v + 3))
				.allMatch(v -> v < 1_000_000);
	}
	@Benchmark
	public boolean gatherer_map_allMatch_repeat_3_andThen() {
		return integers.stream()
				.gather(map((Integer v) -> v + 1).andThen(map(v -> v + 2)).andThen(map(v -> v + 3)))
				.allMatch(v -> v < 1_000_000);
	}
  @Benchmark
  public boolean gatherer_mapsequential_allMatch_repeat_3_andThen() {
    return integers.stream()
        .gather(mapSequential((Integer v) -> v + 1).andThen(mapSequential(v -> v + 2)).andThen(mapSequential(v -> v + 3)))
        .allMatch(v -> v < 1_000_000);
  }
	@Benchmark
	public boolean gatherer_mapsublcass_allMatch_repeat_3_andThen() {
		return integers.stream()
				.gather(mapSubclass((Integer v) -> v + 1).andThen(mapSubclass(v -> v + 2)).andThen(mapSubclass(v -> v + 3)))
				.allMatch(v -> v < 1_000_000);
	}*/

  /*
	@Benchmark
	public long stream_map_count() {
		return strings.stream().map(String::length).count();
	}
	@Benchmark
	public long stream_mapToInt_count() {
		return strings.stream().mapToInt(String::length).count();
	}
	@Benchmark
	public long gatherer_map_count() {
		return strings.stream().gather(map(String::length)).count();
	}
  @Benchmark
  public long gatherer_mapsequential_count() {
    return strings.stream().gather(mapSequential(String::length)).count();
  }

	@Benchmark
	public List<Integer> stream_map_toList() {
		return strings.stream().map(String::length).toList();
	}
	@Benchmark
	public List<Integer> gatherer_map_toList() {
		return strings.stream().gather(map(String::length)).toList();
	}
  @Benchmark
  public List<Integer> gatherer_mapsequential_toList() {
    return strings.stream().gather(mapSequential(String::length)).toList();
  }*/
}
