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
package org.solid.testharness.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;

public class ClientResource implements QuarkusTestResourceLifecycleManager {
    private WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .dynamicPort());
        wireMockServer.start();

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/get/404"))
                .willReturn(WireMock.aResponse().withStatus(404)));

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/get/fault"))
                .willReturn(WireMock.aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/get/turtle"))
                .withHeader(HttpConstants.HEADER_AUTHORIZATION, absent())
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_TEXT_TURTLE)
                        .withBody("TURTLE-NOAUTH")));

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/get/turtle"))
                .withHeader(HttpConstants.HEADER_AUTHORIZATION, containing(HttpConstants.PREFIX_DPOP))
                .withHeader(HttpConstants.HEADER_AUTHORIZATION, matching(".*"))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_TEXT_TURTLE)
                        .withBody("TURTLE-DPOP")));

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/get/turtle"))
                .withHeader(HttpConstants.HEADER_AUTHORIZATION, containing(HttpConstants.PREFIX_BEARER))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_TEXT_TURTLE)
                        .withBody("TURTLE-BEARER")));

        wireMockServer.stubFor(WireMock.put(WireMock.urlEqualTo("/put"))
                .withHeader(HttpConstants.HEADER_CONTENT_TYPE, containing(HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .withRequestBody(containing("TEXT"))
                .willReturn(WireMock.aResponse().withStatus(200)));

        wireMockServer.stubFor(WireMock.put(WireMock.urlEqualTo("/put"))
                .withHeader(HttpConstants.HEADER_CONTENT_TYPE, containing(HttpConstants.MEDIA_TYPE_TEXT_TURTLE))
                .withRequestBody(containing("TURTLE"))
                .willReturn(WireMock.aResponse().withStatus(201)));

        wireMockServer.stubFor(WireMock.head(WireMock.urlEqualTo("/head"))
                .willReturn(WireMock.aResponse()
                        .withHeader("HEADER", "VALUE1", "VALUE2")
                        .withStatus(200)));

        wireMockServer.stubFor(WireMock.delete(WireMock.urlEqualTo("/delete"))
                .willReturn(WireMock.aResponse().withStatus(204)));

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }

    @Override
    public void inject(final Object testInstance) {
        // pass the wiremock base uri back into the test
        if (testInstance instanceof ClientTest) {
            ((ClientTest) testInstance).setBaseUri(URI.create(wireMockServer.baseUrl() + "/"));
        }
    }
}
