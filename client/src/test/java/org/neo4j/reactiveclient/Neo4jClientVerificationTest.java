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

import static org.testcontainers.containers.wait.strategy.Wait.forLogMessage;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;

import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.GenericContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import reactor.core.publisher.Mono;

/**
 * @author Michael J. Simons
 */
public class Neo4jClientVerificationTest extends PublisherVerification<Record> {

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	/**
	 * This is the number required to run all required tests (especially for 3.17).
	 */
	private final static long MAX_NUMBER_OF_RECORDS = 2_147_483_648L;

	private final static Duration TIMEOUT = Duration.ofSeconds(21);
	private final static Duration TIMEOUT_FOR_NO_SIGNALS = Duration.ofSeconds(1);

	private final static String QUERY = "UNWIND RANGE(1, $numberOfRecords) AS n RETURN 'String Number' + n";

	private static GenericContainer neo4j;
	private static Neo4jClient neo4jClient;

	@BeforeClass
	public static void initializeNeo4j() throws URISyntaxException {

		neo4j = new GenericContainer("neo4j:3.4.7") // TODO Derive from Maven properties
			.withExposedPorts(7687)
			.withEnv("NEO4J_AUTH", "none")
			.waitingFor(forLogMessage(".*Bolt enabled on 0\\.0\\.0\\.0:7687\\.\n", 1));
		neo4j.start();

		var boltURI = new URI("bolt://" + neo4j.getContainerIpAddress() + ":" + neo4j.getMappedPort(7687));
		neo4jClient = Neo4jClients.create(GraphDatabase.driver(boltURI));
	}

	public Neo4jClientVerificationTest() {
		super(new TestEnvironment(TIMEOUT.toMillis(), TIMEOUT_FOR_NO_SIGNALS.toMillis()));
	}

	@Override
	public long maxElementsFromPublisher() {
		return MAX_NUMBER_OF_RECORDS;
	}

	@Override
	public Publisher<Record> createPublisher(long l) {
		return neo4jClient.execute(QUERY, Map.of("numberOfRecords", l));
	}

	@Override
	public Publisher<Record> createFailedPublisher() {
		return neo4jClient.execute("invalid");
	}

	@AfterClass
	static void tearDownNeo4j() {
		Mono.from(neo4jClient.close())
			.then(Mono.fromRunnable(neo4j::stop))
			.block();
	}

}