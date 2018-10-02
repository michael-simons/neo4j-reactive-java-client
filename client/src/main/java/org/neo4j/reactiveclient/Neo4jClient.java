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

import org.neo4j.driver.v1.Record;
import org.reactivestreams.Publisher;

import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

/**
 * The client side representation of a Neo4j instance or cluster, connected through an instance of Neo4js Java-Driver.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
public interface Neo4jClient {
	/**
	 * Closes the client and the underlying driver instance.
	 *
	 * @return A publisher signaling successful closing on completion.
	 */
	Publisher<Void> close();

	Publisher<Record> execute(@NonNull String query);

	Publisher<Record> execute(@NonNull String query, @Nullable Map<String, Object> parameter);
}
