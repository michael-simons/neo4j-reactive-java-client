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

import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.v1.GraphDatabase;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Neo4jClientIntegrationTest {

	private GenericContainer neo4j;
	private URI boltURI;

	@BeforeAll
	void initializeNeo4j() throws URISyntaxException {

		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		this.neo4j = new GenericContainer("neo4j:3.4.7") // TODO Derive from Maven properties
			.withExposedPorts(7687)
			.withEnv("NEO4J_AUTH", "none")
			.waitingFor(new HostPortWaitStrategy());
		this.neo4j.start();
		this.boltURI = new URI("bolt://" + this.neo4j.getContainerIpAddress() + ":" + this.neo4j.getMappedPort(7687));
	}

	@Test
	void test() {

		var client = Neo4jClients.create(GraphDatabase.driver(this.boltURI));
		Mono.from(client.close()).subscribe();
	}

	@AfterAll
	void tearDownNeo4j() {
		this.neo4j.stop();
	}
}