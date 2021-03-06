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
package org.solid.testharness.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClient;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolidResourceTest {
    private static final String TEST_URL = "http://localhost/resource";
    URI testUrl = URI.create(TEST_URL);
    URI aclUrl = URI.create(testUrl + ".acl");
    SolidClient solidClient;

    @BeforeEach
    void setUp() {
        solidClient = mock(SolidClient.class);
    }

    @Test
    void testConstructorExceptions() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResource(null, null, null, null)
        );
        assertEquals("Parameter solidClient is required", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResource(solidClient, null, null, null)
        );
        assertEquals("Parameter url is required", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResource(solidClient, "", null, null)
        );
        assertEquals("Parameter url is required", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResource(solidClient, "test", null, null)
        );
        assertEquals("The url must be absolute", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResource(solidClient, TEST_URL, "", null)
        );
        assertEquals("Parameter type is required", exception.getMessage());
    }

    @Test
    void testConstructorNoBody() {
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        assertTrue(resource.exists());
    }

    @Test
    void testCreate() {
        final SolidResource resource = SolidResource.create(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertTrue(resource.exists());
    }

    @Test
    void resourceExistsWithAcl() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        when(solidClient.getAclLink(headers)).thenReturn(aclUrl);
        when(solidClient.getResourceAclLink(testUrl)).thenReturn(aclUrl);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertTrue(resource.exists());
        assertEquals(aclUrl.toString(), resource.getAclUrl());
    }

    @Test
    void resourceExistsWithoutAcl() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + aclUrl + ">; rel=\"noacl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        when(solidClient.getResourceAclLink(testUrl)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertTrue(resource.exists());
        assertNull(resource.getAclUrl());
    }

    @Test
    void resourceExistsNoLink() throws Exception {
        final Map<String, List<String>> headerMap = Collections.emptyMap();
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertTrue(resource.exists());
        assertNull(resource.getAclUrl());
    }

    @Test
    void resourceExistsWithAclAfterLookup() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final URI lookedUpAclUrl = URI.create("http://localhost/lookupedUpAcl");
        when(solidClient.getResourceAclLink(testUrl)).thenReturn(lookedUpAclUrl);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertTrue(resource.exists());
        assertEquals(lookedUpAclUrl.toString(), resource.getAclUrl());
    }

    @Test
    void resourceNotExists() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenThrow(new Exception());

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertFalse(resource.exists());
    }

    @Test
    void getContainer() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        final SolidContainer container = resource.getContainer();

        assertTrue(resource.exists());
        assertFalse(resource.isContainer());
        assertTrue(container.exists());
        assertEquals("http://localhost/", container.getUrl());
        assertEquals("/", container.getPath());
    }

    @Test
    void getParent() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, "http://localhost/container/resource",
                "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        final SolidContainer container = resource.getContainer();
        final SolidContainer nextContainer = container.getContainer();

        assertTrue(resource.exists());
        assertTrue(container.exists());
        assertEquals("http://localhost/container/", container.getUrl());
        assertEquals("/container/", container.getPath());
        assertTrue(nextContainer.exists());
        assertEquals("http://localhost/", nextContainer.getUrl());
        assertEquals("/", nextContainer.getPath());
    }

    @Test
    void getTopLevelContainer() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        final SolidContainer container = resource.getContainer();
        final SolidContainer nextContainer = container.getContainer();

        assertTrue(resource.exists());
        assertTrue(container.exists());
        assertEquals("http://localhost/", container.getUrl());
        assertEquals("/", container.getPath());
        assertNull(nextContainer);
    }

    @Test
    void setAclNoUrl() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertFalse(resource.setAcl("acl"));
    }

    @Test
    void setAclMissing() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        when(solidClient.getAclLink(headers)).thenReturn(null);
        when(solidClient.getResourceAclLink(testUrl)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertFalse(resource.setAcl("acl"));
        // second attempt short cuts as false
        assertFalse(resource.setAcl("acl"));
    }

    @Test
    void setAcl() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        when(solidClient.getAclLink(headers)).thenReturn(aclUrl);
        when(solidClient.getResourceAclLink(testUrl)).thenReturn(aclUrl);
        when(solidClient.createAcl(aclUrl, "acl")).thenReturn(true);

        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertTrue(resource.setAcl("acl"));
    }

    @Test
    void getUrl() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals(TEST_URL, resource.getUrl());
        assertEquals("/resource", resource.getPath());
    }

    @Test
    void getUrlNull() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenThrow(new Exception("Failed as expected"));
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertFalse(resource.exists());
        assertNull(resource.getUrl());
        assertNull(resource.getPath());
    }

    @Test
    void getAclUrlMissing() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        when(solidClient.getAclLink(headers)).thenReturn(null);
        when(solidClient.getResourceAclLink(testUrl)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertNull(resource.getAclUrl());
        // second attempt short cuts
        assertNull(resource.getAclUrl());
    }

    @Test
    void getContentAsTurtle() throws Exception {
        when(solidClient.getContentAsTurtle(testUrl)).thenReturn("CONTENT");
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        assertEquals("CONTENT", resource.getContentAsTurtle());
    }

    @Test
    void getContentAsTurtleEmpty() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenThrow(new Exception("FAIL"));
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(),"hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals("", resource.getContentAsTurtle());
    }

    @Test
    void getAccessControls() throws Exception {
        when(solidClient.getResourceAclLink(testUrl)).thenReturn(aclUrl);
        when(solidClient.getAcl(aclUrl)).thenReturn("ACL");
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        assertEquals("ACL", resource.getAccessControls());
    }

    @Test
    void getAccessControlsMissing() throws Exception {
        when(solidClient.getResourceAclLink(testUrl)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        assertEquals("", resource.getAccessControls());
    }

    @Test
    void delete() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertDoesNotThrow(() -> resource.delete());
    }

    @Test
    void deleteException() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        doThrow(new Exception("Failed as expected")).when(solidClient).deleteResourceRecursively(testUrl);
        assertThrows(Exception.class, () -> resource.delete());
    }

    @Test
    void resourceToString() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals("SolidResource: " + TEST_URL, resource.toString());
    }

    @Test
    void emptyResourceToString() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenThrow(new Exception("Failed as expected"));
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals("SolidResource: null", resource.toString());
    }

    @Test
    void containerToString() throws Exception {
        when(solidClient.createResource(new URI("http://localhost/container/"), null, null))
                .thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, "http://localhost/container/", null, null);
        assertEquals("SolidContainer: http://localhost/container/", resource.toString());
    }
}
