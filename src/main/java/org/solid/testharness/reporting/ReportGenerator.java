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
package org.solid.testharness.reporting;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.RDF;
import org.solid.common.vocab.SPEC;
import org.solid.common.vocab.TD;
import org.solid.testharness.utils.DataRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

    @Inject
    DataRepository dataRepository;

    @Location("coverage-report.html")
    Template coverageTemplate;
    @Location("result-report.html")
    Template resultTemplate;

    private TestSuiteResults testSuiteResults;
    private long startTime;

    public void buildTurtleReport(final Writer writer) throws Exception {
        dataRepository.export(writer);
    }

    public void printReportToConsole() throws Exception {
        final StringWriter sw = new StringWriter();
        dataRepository.export(sw);
        logger.info("REPORT\n{}", sw);
    }

    public void buildHtmlCoverageReport(final Writer writer) throws IOException {
        writer.write(coverageTemplate.data(new ResultData(getSpecifications(), getTestCases(), null)).render());
        writer.flush();
    }

    public void buildHtmlResultReport(final Writer writer) throws IOException {
        writer.write(resultTemplate.data(new ResultData(getSpecifications(), getTestCases(), testSuiteResults))
                .render());
        writer.flush();
    }

    private List<IRI> getSpecifications() {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            return conn.getStatements(null, RDF.type, SPEC.Specification).stream()
                    .map(Statement::getSubject)
                    .map(IRI.class::cast)
                    .collect(Collectors.toList());
        }
    }

    private List<IRI> getTestCases() {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            return conn.getStatements(null, RDF.type, TD.TestCase).stream()
                    .map(Statement::getSubject)
                    .map(IRI.class::cast)
                    .collect(Collectors.toList());
        }
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    public void setResults(final TestSuiteResults testSuiteResults) {
        this.testSuiteResults = testSuiteResults;
        this.testSuiteResults.setStartTime(startTime);
    }
}
