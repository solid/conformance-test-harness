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
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.SPEC;
import org.solid.testharness.utils.AbstractDataModelTests;

import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SpecificationRequirementTest extends AbstractDataModelTests {
    private static final IRI REQUIREMENT = iri(NS, "specification#requirement");
    private static final IRI REQUIREMENT2 = iri(NS, "specification#requirement2");

    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/specification-requirement-testing-feature.ttl";
    }

    @Test
    void getConformanceRole() {
        final SpecificationRequirement requirement = new SpecificationRequirement(REQUIREMENT);
        assertEquals(SPEC.ServerConformanceClass.stringValue(), requirement.getConformanceRole());
    }

    @Test
    void getConformanceLevel() {
        final SpecificationRequirement requirement = new SpecificationRequirement(REQUIREMENT);
        assertEquals(SPEC.ConformanceMust.stringValue(), requirement.getConformanceLevel());
    }

    @Test
    void getExcerpt() {
        final SpecificationRequirement requirement = new SpecificationRequirement(REQUIREMENT);
        assertEquals("excerpt of requirement 1", requirement.getExcerpt());
    }

    @Test
    void getTestCase() {
        final SpecificationRequirement requirement = new SpecificationRequirement(REQUIREMENT);
        final List<TestCase> testCases = requirement.getTestCases();
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals("Group 1", testCases.get(0).getTitle());
    }

    @Test
    void getTestCaseMissing() {
        final SpecificationRequirement requirement = new SpecificationRequirement(REQUIREMENT2);
        final List<TestCase> testCases = requirement.getTestCases();
        assertNull(testCases);
    }
}
