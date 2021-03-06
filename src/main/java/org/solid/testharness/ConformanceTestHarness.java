/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.solid.testharness;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.DOAP;
import org.solid.common.vocab.EARL;
import org.solid.common.vocab.RDF;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.reporting.ReportGenerator;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.Namespaces;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
@SuppressWarnings("PMD.MoreThanOneLogger")  // Additional logger provided for JSON output
public class ConformanceTestHarness {
    private static final Logger logger = LoggerFactory.getLogger(ConformanceTestHarness.class);
    private static final Logger resultLogger = LoggerFactory.getLogger("ResultLogger");

    @Inject
    Config config;
    @Inject
    TestSubject testSubject;
    @Inject
    TestSuiteDescription testSuiteDescription;
    @Inject
    TestRunner testRunner;
    @Inject
    ReportGenerator reportGenerator;
    @Inject
    DataRepository dataRepository;

    @SuppressWarnings("PMD.UseProperClassLoader") // this is not J2EE and the suggestion fails
    public void initialize() throws IOException {
        reportGenerator.setStartTime(System.currentTimeMillis());
        try (final InputStream is = getClass().getClassLoader().getResourceAsStream("assertor.properties")) {
            final Properties properties = new Properties();
            properties.load(is);
            final IRI assertor = iri(Namespaces.TEST_HARNESS_URI);
            dataRepository.setAssertor(assertor);
            try (RepositoryConnection conn = dataRepository.getConnection()) {
                final ModelBuilder builder = new ModelBuilder();
                final BNode bnode = Values.bnode();
                conn.add(builder.subject(assertor)
                        .add(RDF.type, EARL.Software)
                        .add(DOAP.name, properties.getProperty("package.name"))
                        .add(DOAP.description, properties.getProperty("package.description"))
                        .add(DOAP.created, Date.from(
                                Instant.from(DateTimeFormatter.ISO_INSTANT
                                        .parse(properties.getProperty("package.buildTime")))
                        ))
                        .add(DOAP.developer, iri(properties.getProperty("package.organizationUrl")))
                        .add(DOAP.homepage, iri(properties.getProperty("package.url")))
                        .add(DOAP.release, bnode)
                        .add(bnode, DOAP.revision, properties.getProperty("package.version"))
                        .build());
            }
        }
        config.logConfigSettings();
        testSuiteDescription.load(config.getTestSources());
    }

    public boolean createCoverageReport() {
        logger.info("===================== DISCOVER TESTS ========================");
        try {
            final List<IRI> testCases = testSuiteDescription.getAllTestCases();
            final List<String> featurePaths = testSuiteDescription.locateTestCases(testCases);
            if (featurePaths.isEmpty()) {
                logger.warn("There are no tests available");
                return true;
            }
        } catch (TestHarnessInitializationException e) {
            logger.error("Cannot build report", e);
            return false;
        }

        logger.info("===================== BUILD REPORT ========================");
        final File outputDir = config.getOutputDirectory();
        logger.info("Reports location: {}", outputDir.getPath());
        try {
            final File coverageHtmlFile = new File(outputDir, "coverage.html");
            logger.info("Coverage report HTML/RDFa file: {}", coverageHtmlFile.getPath());
            reportGenerator.buildHtmlCoverageReport(Files.newBufferedWriter(coverageHtmlFile.toPath()));
            return true;
        } catch (IOException e) {
            logger.error("Failed to write coverage report", e);
            return false;
        }
    }

    public TestSuiteResults runTestSuites() {
        return this.runTestSuites(null);
    }

    public TestSuiteResults runTestSuites(final List<String> filters) {
        logger.info("===================== DISCOVER TESTS ========================");
        final List<String> featurePaths;
        try {
            testSubject.loadTestSubjectConfig();
            // TODO: Consider running some initial tests to discover the features provided by a server
            final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(
                    testSubject.getTargetServer().getFeatures().keySet()
            );
            logger.info("==== TEST CASES FOUND: {} - {}", testCases.size(), testCases);

            final List<IRI> filteredTestCases = testCases.stream()
                    .filter(tc -> filters == null || filters.isEmpty() ||
                            filters.stream().anyMatch(f -> tc.stringValue().contains(f))
                    )
                    .collect(Collectors.toList());
            logger.info("==== FILTERED TEST CASES: {} - {}", filteredTestCases.size(), filteredTestCases);

            featurePaths = testSuiteDescription.locateTestCases(filteredTestCases);
            if (featurePaths.isEmpty()) {
                logger.warn("There are no tests available");
                return null;
            } else {
                logger.info("==== RUNNING {} TEST CASES: {}", featurePaths.size(), featurePaths);
            }

            logger.info("===================== REGISTER CLIENTS ========================");
            logger.info("Test subject: {}", config.getServerRoot());
            if (config.getUserRegistrationEndpoint() != null) {
                testSubject.registerUsers();
            }
            testSubject.registerClients();
            testSubject.prepareServer();
        } catch (TestHarnessInitializationException e) {
            logger.error("Cannot run test suites", e);
            return null;
        }

        logger.info("===================== RUN TESTS ========================");
        final TestSuiteResults results = testRunner.runTests(featurePaths, config.getMaxThreads());
        reportGenerator.setResults(results);

        if (!config.isSkipTearDown()) {
            logger.info("===================== DELETING TEST RESOURCES ========================");
            testSubject.tearDownServer();
        }

        logger.info("===================== BUILD REPORTS ========================");
        final File outputDir = config.getOutputDirectory();
        logger.info("Reports location: {}", outputDir.getPath());
        try {
            resultLogger.info(results.toJson());
            final File reportTurtleFile = new File(outputDir, "report.ttl");
            logger.info("Report Turtle file: {}", reportTurtleFile.getPath());
            reportGenerator.buildTurtleReport(Files.newBufferedWriter(reportTurtleFile.toPath()));

            final File reportHtmlFile = new File(outputDir, "report.html");
            logger.info("Report HTML/RDFa file: {}", reportHtmlFile.getPath());
            reportGenerator.buildHtmlResultReport(Files.newBufferedWriter(reportHtmlFile.toPath()));

            final File coverageHtmlFile = new File(outputDir, "coverage.html");
            logger.info("Coverage report HTML/RDFa file: {}", coverageHtmlFile.getPath());
            reportGenerator.buildHtmlCoverageReport(Files.newBufferedWriter(coverageHtmlFile.toPath()));
        } catch (Exception e) {
            logger.error("Failed to write reports", e);
        }

        logger.info("{}", results);
        return results;
    }
}
