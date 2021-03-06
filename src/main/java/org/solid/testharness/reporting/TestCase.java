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

import org.eclipse.rdf4j.model.IRI;
import org.solid.common.vocab.*;
import org.solid.testharness.utils.DataModelBase;

import java.util.List;

public class TestCase extends DataModelBase {
    public TestCase(final IRI subject) {
        super(subject, ConstructMode.INC_REFS);
    }

    public String getTitle() {
        return getLiteralAsString(DCTERMS.title);
    }

    public String getStatus() {
        return getIriAsString(TD.reviewStatus);
    }

    public String getTestScript() {
        return getIriAsString(SPEC.testScript);
    }

    public String getRequirementReference() {
        return getIriAsString(SPEC.requirementReference);
    }

    public boolean isImplemented() {
        return getIriAsString(SPEC.testScript) != null;
    }

    public Assertion getAssertion() {
        final List<Assertion> assertions = getModelListByObject(EARL.test, Assertion.class);
        if (assertions != null) {
            return assertions.get(0);
        } else {
            return null;
        }
    }

    public List<Scenario> getScenarios() {
        return getModelList(DCTERMS.hasPart, Scenario.class);
    }
}
