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

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.AbstractDataModelTests;

import java.time.LocalDate;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TestSubjectTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/testsubject-testing-feature.ttl";
    }

    @Test
    void getSoftwareName() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject1"));
        assertEquals("TESTER1", testSubject.getSoftwareName());
    }

    @Test
    void getDescription() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject1"));
        assertEquals("DESCRIPTION", testSubject.getDescription());
    }

    @Test
    void getCreatedDate() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject1"));
        assertEquals(LocalDate.parse("2021-03-05"), testSubject.getCreatedDate());
    }

    @Test
    void getProgrammingLanguage() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject1"));
        assertEquals("LANG", testSubject.getProgrammingLanguage());
    }

    @Test
    void getDeveloper() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject1"));
        assertEquals("https://example.org/profile/card/#us", testSubject.getDeveloper());
    }

    @Test
    void getHomepage() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject1"));
        assertEquals("https://example.org/testSubject1", testSubject.getHomepage());
    }

    @Test
    void getRevision() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject1"));
        assertEquals("0.0.1-SNAPSHOT", testSubject.getRevision());
    }

    @Test
    void getReleaseName() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject1"));
        assertEquals("RELEASE_NAME", testSubject.getReleaseName());
    }

    @Test
    void getCreatedDateNull() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject2"));
        assertNull(testSubject.getCreatedDate());
    }

    @Test
    void getReleaseNameNull() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject2"));
        assertNull(testSubject.getReleaseName());
    }

    @Test
    void getRevisionNull() {
        final TestSubject testSubject = new TestSubject(iri(NS, "testSubject2"));
        assertNull(testSubject.getRevision());
    }
}
