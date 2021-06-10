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
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.solid.common.vocab.*;
import org.solid.testharness.utils.Namespaces;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;

public class ResultData {
    private Assertor assertor;
    private List<Specification> specifications;

    public ResultData(final List<IRI> specifications) {
        assertor = new Assertor(iri(Namespaces.TEST_HARNESS_URI));
        this.specifications = specifications.stream().map(Specification::new).collect(Collectors.toList());
    }

    public String getPrefixes() {
        return Namespaces.generateHtmlPrefixes(List.of(RDF.PREFIX, RDFS.PREFIX, XSD.PREFIX, DCTERMS.PREFIX, DOAP.PREFIX,
                SOLID.PREFIX, SOLID_TEST.PREFIX, EARL.PREFIX, TD.PREFIX, SPEC.PREFIX));
    }

    public List<Specification> getSpecifications() {
        return specifications;
    }

    public Assertor getAssertor() {
        return assertor;
    }
}
