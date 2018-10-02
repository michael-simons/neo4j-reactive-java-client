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

import java.util.function.Function;

import org.neo4j.driver.v1.Record;

/**
 * Bunch of utilities for the examples..
 *
 * @author Michael J. Simons
 */
public interface Examples {
	static Function<Record, String> recordToString() {
		return r -> r.get("n").asNode().labels().toString();
	}
}