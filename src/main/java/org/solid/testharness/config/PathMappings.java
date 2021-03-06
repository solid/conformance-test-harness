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
package org.solid.testharness.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithParentName;
import org.eclipse.microprofile.config.spi.Converter;
import org.eclipse.rdf4j.model.IRI;
import org.solid.testharness.http.HttpUtils;
import org.solid.testharness.utils.TestHarnessInitializationException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ConfigMapping(prefix = "mappings")
public interface PathMappings {
    @WithParentName
    List<PathMapping> mappings();

    default String stringValue() {
        return mappings().stream().map(PathMapping::stringValue).collect(Collectors.toList()).toString();
    }

    default IRI unmapFeaturePath(final String path) {
        URI uri = URI.create(path);
        if (HttpUtils.isHttpProtocol(uri.getScheme())) {
            return iri(uri.toString());
        }
        if (!HttpUtils.isFileProtocol(uri.getScheme())) {
            uri = Path.of(path).toAbsolutePath().normalize().toUri();
        }
        final String finalPath = uri.toString();
        final PathMapping mapping = mappings().stream()
                .filter(m -> finalPath.startsWith(m.path())).findFirst().orElse(null);
        return mapping != null ? iri(finalPath.replace(mapping.path(), mapping.prefix())) : null;
    }

    default URI mapFeatureIri(final IRI featureIri) {
        final String location = featureIri.stringValue();
        final PathMapping mapping = mappings().stream()
                .filter(m -> location.startsWith(m.prefix())).findFirst().orElse(null);
        return URI.create(mapping != null ? location.replace(mapping.prefix(), mapping.path()) : location);
    }

    default URL mapIri(final IRI iri) {
        return toUrl(iri.stringValue());
    }

    default URL mapUrl(final URL url) {
        return toUrl(url.toString());
    }

    default URL toUrl(final String location) {
        final PathMapping mapping = mappings().stream()
                .filter(m -> location.startsWith(m.prefix())).findFirst().orElse(null);
        try {
            return new URL(
                    mapping != null ? location.replace(mapping.prefix(), mapping.path()) : location);
        } catch (MalformedURLException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException("Bad URL mapping: %s",
                    e.toString()).initCause(e);
        }
    }

//    public void setMappings(@NotNull final List<PathMapping> mappings) {
//        requireNonNull(mappings, "mappings must not be null");
//        if (mappings.size() == 1 && mappings.get(0) == null) {
//            this.mappings = Collections.emptyList();
//        } else {
//            this.mappings = mappings;
//        }
//    }

    interface PathMapping {
        @WithConverter(PrefixConverter.class)
        String prefix();

        @WithConverter(PathConverter.class)
        String path();

        default String stringValue() {
            return String.format("%s => %s", prefix(), path());
        }
//        public static PathMapping create(final String prefix, final String path) {
//            final PathMapping mapping = new PathMapping();
//            mapping.setPrefix(prefix);
//            mapping.setPath(path);
//            return mapping;
//        }
    }

    class PathConverter implements Converter<String> {
        private static final long serialVersionUID = 6105901390387650548L;

        @Override
        public String convert(final String value) {
            String path;
            if (value.matches("^(https?|file):/.*")) {
                path = value;
            } else {
                path = Path.of(value).toAbsolutePath().normalize().toUri().toString();
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return path;
        }
    }

    class PrefixConverter implements Converter<String> {
        private static final long serialVersionUID = -5363627210557105464L;

        @Override
        public String convert(final String value) {
            if (value.endsWith("/")) {
                return value.substring(0, value.length() - 1);
            } else {
                return value;
            }
        }
    }
}
