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
package org.neo4j.reactiveclient.examples;

import static org.neo4j.reactiveclient.examples.Examples.recordToString;

import java.util.concurrent.atomic.AtomicInteger;

import org.neo4j.driver.v1.Record;
import org.neo4j.reactiveclient.Neo4jClients;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Simple Hello World.
 *
 * @author Michael J. Simons
 */
public class HelloWorldExampleUsingReactiveStreams {

	private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldExampleUsingReactiveStreams.class);

	public static void main(final String... args) {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		var client = Neo4jClients.create("bolt://localhost:7687", "neo4j", "music");
		var records = client.execute("MATCH (n) RETURN n");

		records.subscribe(new AbstractLimitingSubscriber<>(5) {
			@Override
			void next(final Record record) {
				LOGGER.info(recordToString().apply(record));
			}

			@Override
			void canceledOrCompleted() {
				client.close().subscribe(new AbstractLimitingSubscriber<>(1) {
					@Override
					void next(final Void element) {
					}

					@Override
					void canceledOrCompleted() {
						LOGGER.info("Client was closed.");
					}
				});
			}
		});
	}

	abstract static class AbstractLimitingSubscriber<T> implements Subscriber<T> {
		final AtomicInteger requestsPending;
		private Subscription subscription;

		AbstractLimitingSubscriber(final int limit) {
			this.requestsPending = new AtomicInteger(limit);
		}

		void request() {
			final int demand = requestsPending.get();
			if (demand > 0) {
				requestsPending.compareAndSet(demand, demand - 1);
				this.subscription.request(1);
			} else {
				this.subscription.cancel();
				this.canceledOrCompleted();
			}
		}

		@Override
		public final void onSubscribe(final Subscription s) {
			this.subscription = s;
			request();
		}

		@Override
		public final void onNext(final T element) {
			next(element);
			request();
		}

		@Override
		public void onError(final Throwable t) {
		}

		@Override
		public void onComplete() {
			this.canceledOrCompleted();
		}

		abstract void next(T element);

		abstract void canceledOrCompleted();
	}
}
