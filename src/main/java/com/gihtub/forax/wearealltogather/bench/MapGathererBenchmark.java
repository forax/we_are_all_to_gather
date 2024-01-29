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

// Benchmark                                   Mode  Cnt    Score    Error  Units

// MapGathererBenchmark.stream_map_sum         avgt    5  481.222 ±  1.560  us/op
// MapGathererBenchmark.stream_mapToInt_sum    avgt    5  102.089 ±  0.672  us/op
// MapGathererBenchmark.gatherer_map_sum       avgt    5  552.384 ±  3.405  us/op

// MapGathererBenchmark.stream_map_count       avgt    5    0.009 ±  0.001  us/op
// MapGathererBenchmark.stream_mapToInt_count  avgt    5    0.009 ±  0.001  us/op
// MapGathererBenchmark.gatherer_map_count     avgt    5  101.993 ±  0.105  us/op

// MapGathererBenchmark.stream_map_toList      avgt    5  332.322 ±  0.512  us/op
// MapGathererBenchmark.gatherer_map_toList    avgt    5  558.873 ±  6.200  us/op

@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = { "--enable-preview" })
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@SuppressWarnings("static-method")
public class MapGathererBenchmark {

	private final List<String> values = IntStream.range(0, 100_000).mapToObj(i -> "item" + i).toList();

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
		return values.stream().map(String::length).reduce(0, Integer::sum);
	}
	@Benchmark
	public int stream_mapToInt_sum() {
		return values.stream().mapToInt(String::length).sum();
	}
	@Benchmark
	public int gatherer_map_sum() {
		return values.stream().gather(map(String::length)).reduce(0, Integer::sum);
	}
	@Benchmark
	public int gatherer_mapsublcass_sum() {
		return values.stream().gather(mapSubclass(String::length)).reduce(0, Integer::sum);
	}*/

	@Benchmark
	public boolean stream_map_allMatch() {
		return values.stream().map(String::length).allMatch(v -> v < 10);
	}
	@Benchmark
	public boolean stream_mapToInt_allMatch() {
		return values.stream().mapToInt(String::length).allMatch(v -> v < 10);
	}
	@Benchmark
	public boolean gatherer_map_allMatch() {
		return values.stream().gather(map(String::length)).allMatch(v -> v < 10);
	}
	@Benchmark
	public boolean gatherer_mapsublcass_allMatch() {
		return values.stream().gather(mapSubclass(String::length)).allMatch( v -> v < 10);
	}

	/*
	@Benchmark
	public long stream_map_count() {
		return values.stream().map(String::length).count();
	}
	@Benchmark
	public long stream_mapToInt_count() {
		return values.stream().mapToInt(String::length).count();
	}
	@Benchmark
	public long gatherer_map_count() {
		return values.stream().gather(map(String::length)).count();
	}

	@Benchmark
	public List<Integer> stream_map_toList() {
		return values.stream().map(String::length).toList();
	}
	@Benchmark
	public List<Integer> gatherer_map_toList() {
		return values.stream().gather(map(String::length)).toList();
	}
	*/
}
