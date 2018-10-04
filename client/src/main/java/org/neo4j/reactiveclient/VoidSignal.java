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

/**
 * To be compatible with RxJava 2 we cannot emit Void from any Publisher.
 * See <a href="https://github.com/ReactiveX/RxJava/wiki/What%27s-different-in-2.0#nulls">RxJava 2 on Nulls</a>.
 * Therefor we emmit a Void Signal each time a publisher may only return a single, void item.
 *
 * @author Michael J. Simons
 *
 * @since 1.0.0
 */
public enum VoidSignal {
	INSTANCE;
}
