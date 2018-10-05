/*
 * Copyright (c) 2018 "Neo4j Sweden AB" <https://neo4j.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.reactiveclient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResultCursor;
import org.reactivestreams.Subscription;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;
import reactor.util.context.Context;

/**
 * Default implementation of {@link Neo4jClient}.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
final class DefaultNeo4jClientImpl implements Neo4jClient {

	/**
	 * A helper function to map all completation stages that return voids (null) into a {@link VoidSignal}.
	 */
	private static final Function<Void, CompletionStage<VoidSignal>> VOID_TO_VOID_SIGNAL_FUNCTION
		= realVoid -> CompletableFuture.completedFuture(VoidSignal.INSTANCE);

	/**
	 * The actual driver instance.
	 */
	private final Driver driver;

	DefaultNeo4jClientImpl(final Driver driver) {
		this.driver = driver;
	}

	@Override
	public Mono<VoidSignal> close() {

		final Supplier<CompletionStage<VoidSignal>> completionStageSupplier = () -> this.driver.closeAsync()
			.thenCompose(VOID_TO_VOID_SIGNAL_FUNCTION);
		return Mono.fromCompletionStage(completionStageSupplier); // does defer for us.
	}

	@Override
	public Flux<Record> execute(@NonNull final String query) {
		return execute(query, Map.of());
	}

	@Override
	public Flux<Record> execute(@NonNull final String query, @Nullable final Map<String, Object> parameter) {
		return Mono.<StatementResultCursor>create(sink ->
			driver.session().runAsync(query, Optional.ofNullable(parameter).orElseGet(Map::of))
				.whenComplete((cursor, error) -> {
					if (error != null) {
						sink.error(error);
					} else {
						sink.success(cursor);
					}
				})
		).flatMapMany(RecordEmitter::forCursor);
	}

	private static class RecordEmitter {

		private final AtomicLong requestsPending = new AtomicLong();
		private final AtomicBoolean active = new AtomicBoolean(true);
		private final AtomicBoolean emissionScheduled = new AtomicBoolean(false);

		private final StatementResultCursor cursor;

		private FluxSink<Record> sink;

		static Flux<Record> forCursor(final StatementResultCursor cursor) {

			var emitter = new RecordEmitter(cursor);
			return Flux.push(emitter::emitOn);
		}

		private RecordEmitter(final StatementResultCursor cursor) {
			this.cursor = cursor;
		}

		private void emitOn(final FluxSink<Record> targetSink) {

			this.sink = targetSink;
			this.sink.onRequest(toAdd -> {
				if (this.isActive()) {
					long r = this.getRequested();
					if (r == Long.MAX_VALUE) {
						scheduleEmission();
					}
					long u = Operators.addCap(r, toAdd);
					if (this.setRequested(r, u)) {
						if (u > 0) {
							scheduleEmission();
						}
					}
				}
			});

			this.sink.onCancel(this::deactivate);
			this.sink.onDispose(this::deactivate);
		}

		private boolean isEmissionScheduled() {
			return emissionScheduled.get();
		}

		private void scheduleEmission() {
			if (this.isEmissionScheduled() || !this.isActive() || this.getRequested() <= 0) {
				return;
			}

			if (emissionScheduled.compareAndSet(false, true)) {
				Mono.fromCompletionStage(() -> cursor.nextAsync()
					.whenComplete((record, throwable) -> {
						if (record == null && throwable == null) {
							sink.complete(); // Signal competition to the upper sink when we reach the end of the result set.
						}
					}))
					.subscribe(new RecordSubscriber());
			}
		}

		private void emissionCompleted() {
			emissionScheduled.compareAndSet(true, false);
		}

		private boolean isActive() {
			return active.get();
		}

		private void deactivate() {
			active.set(false);
		}

		private long getRequested() {
			return requestsPending.get();
		}

		private boolean setRequested(final long expect, final long update) {
			return requestsPending.compareAndSet(expect, update);
		}

		private class RecordSubscriber implements CoreSubscriber<Record> {

			@Override
			public void onSubscribe(final Subscription s) {
				s.request(1);
			}

			@Override
			public void onNext(final Record message) {

				long requested = RecordEmitter.this.getRequested();
				if (requested > 0) {
					sink.next(message);
				}
			}

			@Override
			public void onError(final Throwable t) {

				RecordEmitter.this.deactivate();
				sink.error(t);
			}

			@Override
			public void onComplete() {
				RecordEmitter.this.emissionCompleted();

				scheduleEmission();
			}

			@Override
			public Context currentContext() {
				return sink.currentContext();
			}
		}
	}
}
