package com.gihtub.forax.wearealltogather.bench;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Function;
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



@Warmup(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = { "--enable-preview" })
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
	public int gatherer_mapsublcass_sum() {
		return strings.stream().gather(mapSubclass(String::length)).reduce(0, Integer::sum);
	}
  */

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
	private boolean mapSubclass_check(int add) {
		return strings.stream().gather(mapSubclass(s -> s.charAt(0) + add)).allMatch(v -> v < 128);
	}

	@Benchmark
	public boolean gatherer_map_allMatch() {
		return map_check(1);
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
	public boolean gatherer_mapsublcass__allMatch_3() {
		return mapSubclass_check(1) && mapSubclass_check(2) && mapSubclass_check(3);
	}*/


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
	public boolean gatherer_mapsublcass_allMatch_repeat_3() {
		return integers.stream()
				.gather(mapSubclass(v -> v + 1))
				.gather(mapSubclass(v -> v + 2))
				.gather(mapSubclass(v -> v + 3))
				.allMatch(v -> v < 1_000_000);
	}

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
	public List<Integer> stream_map_toList() {
		return strings.stream().map(String::length).toList();
	}
	@Benchmark
	public List<Integer> gatherer_map_toList() {
		return strings.stream().gather(map(String::length)).toList();
	}
  */
}
