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

import org.neo4j.driver.v1.Driver;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link Neo4jClient}.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
final class DefaultNeo4jClientImpl implements Neo4jClient {
	private final Driver driver;

	DefaultNeo4jClientImpl(final Driver driver) {
		this.driver = driver;
	}

	@Override
	public Publisher<Void> close() {
		return Mono.defer(() -> Mono.fromCompletionStage(this.driver.closeAsync()));
	}

	@Override
	public Publisher<String> selectStuff() {
		return Flux.just("a", "b", "c");
	}
}
