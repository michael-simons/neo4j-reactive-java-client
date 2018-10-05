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

import org.neo4j.reactiveclient.Neo4jClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import io.reactivex.Completable;
import io.reactivex.Flowable;

/**
 * Simple Hello World.
 *
 * @author Michael J. Simons
 */
public class HelloWorldExampleUsingRxJava2 {

	private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldExampleUsingRxJava2.class);

	public static void main(final String... args) {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		var client = Neo4jClients.create("bolt://localhost:7687", "neo4j", "music");

		Flowable.fromPublisher(client.execute("MATCH (n) RETURN n"))
			.take(5)
			.map(r -> recordToString().apply(r)) // This thing doesn't take java.util.function but io.reactivex.functions.Function
			.doOnNext(r -> LOGGER.info(r))
			.concatWith(Completable.fromPublisher(client.close()))
			.doOnComplete(() -> LOGGER.info("Client was closed."))
			.subscribe();
	}
}
