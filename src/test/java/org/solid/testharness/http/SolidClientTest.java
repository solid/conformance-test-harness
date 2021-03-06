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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.ConfigTestNormalProfile;
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestProfile(ConfigTestNormalProfile.class)
class SolidClientTest {
    private static final String PREFIX = "@prefix ldp: <http://www.w3.org/ns/ldp#> .";

    @Inject
    ClientRegistry clientRegistry;

    @Test
    void createDefaultClient() {
        final SolidClient solidClient = new SolidClient();
        final Client client = solidClient.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void createMissingNamedClient() {
        assertThrows(TestHarnessInitializationException.class, () -> new SolidClient("nobody"));
    }

    @Test
    void createWithExistingClient() {
        final Client newClient = new Client.Builder("newuser").build();
        final SolidClient solidClient = new SolidClient(newClient);
        final Client client = solidClient.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("newuser", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void createNamedClient() {
        clientRegistry.register("user1", new Client.Builder("user1").build());
        final SolidClient solidClient = new SolidClient("user1");
        final Client client = solidClient.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("user1", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void createStatically() {
        clientRegistry.register("user2", new Client.Builder("user2").build());
        final SolidClient solidClient = SolidClient.create("user2");
        final Client client = solidClient.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("user2", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void getAuthHeaders() {
        final Client mockClient = mock(Client.class);
        when(mockClient.getAuthHeaders("GET", "http://localhost:3000")).thenReturn(Collections.emptyMap());

        final SolidClient solidClient = new SolidClient(mockClient);
        final var headers = solidClient.getAuthHeaders("GET", "http://localhost:3000");
        assertTrue((headers.isEmpty()));
    }

    @Test
    void createResource() throws Exception {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(200);
        when(mockClient.put(eq(URI.create("http://localhost:3000/test")), eq("DATA"),
                eq(HttpConstants.MEDIA_TYPE_TEXT_PLAIN)))
                .thenReturn(mockResponse);

        final SolidClient solidClient = new SolidClient(mockClient);
        final HttpHeaders headers = solidClient.createResource(URI.create("http://localhost:3000/test"),
                "DATA", HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertTrue(headers.map().isEmpty());
        verify(mockClient).put(URI.create("http://localhost:3000/test"), "DATA", HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
    }

    @Test
    void createResourceFails() throws Exception {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(412);
        when(mockClient.put(eq(URI.create("http://localhost:3000/test")), eq("DATA"),
                eq(HttpConstants.MEDIA_TYPE_TEXT_PLAIN)))
                .thenReturn(mockResponse);

        final SolidClient solidClient = new SolidClient(mockClient);
        final Exception exception = assertThrows(Exception.class,
                () -> solidClient.createResource(URI.create("http://localhost:3000/test"), "DATA",
                        HttpConstants.MEDIA_TYPE_TEXT_PLAIN)
        );
        assertEquals("Failed to create resource - status=412", exception.getMessage());
    }

    @Test
    void createContainer() throws Exception {
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(201, "");
        doReturn(mockResponse).when(mockClient).sendAuthorized(any(), any());

        final SolidClient solidClient = new SolidClient(mockClient);
        final HttpResponse<String> response = solidClient.createContainer(URI.create("http://localhost:3000/test/"));
        assertEquals(201, response.statusCode());
    }

    @Test
    void createContainerFails() throws Exception {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        final HttpHeaders mockHeaders = HttpHeaders.of(Collections.emptyMap(), (k, v) -> true);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockResponse.statusCode()).thenReturn(412);
        when(mockClient.put(eq(URI.create("http://localhost:3000/test")), eq("DATA"),
                eq(HttpConstants.MEDIA_TYPE_TEXT_PLAIN)))
                .thenReturn(mockResponse);

        final SolidClient solidClient = new SolidClient(mockClient);
        final Exception exception = assertThrows(Exception.class,
                () -> solidClient.createResource(URI.create("http://localhost:3000/test"), "DATA",
                        HttpConstants.MEDIA_TYPE_TEXT_PLAIN)
        );
        assertEquals("Failed to create resource - status=412", exception.getMessage());
    }

    @Test
    void getResourceAclLink() throws IOException, InterruptedException {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(204, Map.of(HttpConstants.HEADER_LINK,
                List.of("<http://localhost:3000/test.acl>; rel=\"acl\"")));
        when(mockClient.head(any())).thenReturn(mockResponse);

        final SolidClient solidClient = new SolidClient(mockClient);
        final URI uri = solidClient.getResourceAclLink(URI.create("http://localhost:3000/test"));
        assertEquals(URI.create("http://localhost:3000/test.acl"), uri);
        verify(mockClient).head(URI.create("http://localhost:3000/test"));
    }

    @Test
    void getResourceAclLinkFails() throws IOException, InterruptedException {
        final Client mockClient = mock(Client.class);
        when(mockClient.head(any())).thenThrow(new IOException("Failed"));

        final SolidClient solidClient = new SolidClient(mockClient);
        assertThrows(IOException.class, () -> solidClient.getResourceAclLink(URI.create("http://localhost:3000/test")));
    }

    @Test
    void getAclLink() {
        final Map<String, List<String>> headerMap = Map.of("Link",
                List.of("<http://localhost:3000/test.acl>; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);

        final SolidClient solidClient = new SolidClient();
        final URI uri = solidClient.getAclLink(headers);
        assertEquals(URI.create("http://localhost:3000/test.acl"), uri);
    }

    @Test
    void getAclLinkMissing() {
        final HttpHeaders headers = HttpHeaders.of(Collections.emptyMap(), (k, v) -> true);

        final SolidClient solidClient = new SolidClient();
        assertNull(solidClient.getAclLink(headers));
    }

    @Test
    void getAclLinkMultiple() {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK, List.of(
                "<http://localhost:3000/test.acl>; rel=\"acl\"",
                "<http://localhost:3000/test.acl2>; rel=\"acl\""
                ));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);

        final SolidClient solidClient = new SolidClient();
        final URI uri = solidClient.getAclLink(headers);
        assertEquals(URI.create("http://localhost:3000/test.acl"), uri);
    }

    @Test
    void createAcl() throws IOException, InterruptedException {
        final URI resourceAcl = URI.create("http://localhost:3000/test.acl");
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(200);
        when(mockClient.put(eq(resourceAcl), eq("ACL"), eq(HttpConstants.MEDIA_TYPE_TEXT_TURTLE)))
                .thenReturn(mockResponse);

        final SolidClient solidClient = new SolidClient(mockClient);
        final boolean res = solidClient.createAcl(resourceAcl, "ACL");
        assertTrue(res);
        verify(mockClient).put(resourceAcl, "ACL", HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
    }

    @Test
    void getAcl() throws IOException, InterruptedException {
        final URI resourceAcl = URI.create("http://localhost:3000/test.acl");
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "ACL CONTENT");
        when(mockClient.getAsTurtle(any())).thenReturn(mockResponse);

        final SolidClient solidClient = new SolidClient(mockClient);
        final String res = solidClient.getAcl(resourceAcl);
        assertEquals("ACL CONTENT", res);
    }

    @Test
    void createAclFails() throws IOException, InterruptedException {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(400);
        final URI resourceAcl = URI.create("http://localhost:3000/test.acl");

        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockClient.put(eq(resourceAcl), eq("ACL"), eq(HttpConstants.MEDIA_TYPE_TEXT_TURTLE)))
                .thenReturn(mockResponse);

        final SolidClient solidClient = new SolidClient(mockClient);
        final boolean res = solidClient.createAcl(resourceAcl, "ACL");
        assertFalse(res);
        verify(mockClient).put(resourceAcl, "ACL", HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
    }

    @Test
    void getContainmentData() throws Exception {
        final Client mockClient = mock(Client.class);
        final URI resourceAcl = URI.create("http://localhost:3000/test");
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "TEST");

        when(mockClient.getAsTurtle(eq(resourceAcl))).thenReturn(mockResponse);

        final SolidClient solidClient = new SolidClient(mockClient);
        assertEquals("TEST", solidClient.getContentAsTurtle(resourceAcl));
        verify(mockClient).getAsTurtle(resourceAcl);
    }

    @Test
    void getContainmentDataFails() throws IOException, InterruptedException {
        final Client mockClient = mock(Client.class);
        final URI resourceAcl = URI.create("http://localhost:3000/test");
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(400, null);

        when(mockClient.getAsTurtle(eq(resourceAcl))).thenReturn(mockResponse);

        final SolidClient solidClient = new SolidClient(mockClient);
        final Exception exception = assertThrows(Exception.class, () -> solidClient.getContentAsTurtle(resourceAcl));
        assertEquals("Error response=400 trying to get content for http://localhost:3000/test",
                exception.getMessage());
    }

    @Test
    void parseMembers() throws Exception {
        final String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test/>.";
        final SolidClient solidClient = new SolidClient();
        final List<String> members = solidClient.parseMembers(data, URI.create("http://localhost:3000/test/"));
        assertFalse(members.isEmpty());
        assertEquals("http://localhost:3000/test/", members.get(0));
    }

    @Test
    void parseMembersEmpty() throws Exception {
        final String data = PREFIX + "<http://localhost:3000/> a ldp:Container.";
        final SolidClient solidClient = new SolidClient();
        final List<String> members = solidClient.parseMembers(data, URI.create("http://localhost:3000/test/"));
        assertTrue(members.isEmpty());
    }

    @Test
    void parseMembersFails() {
        final SolidClient solidClient = new SolidClient();
        final Exception exception = assertThrows(Exception.class,
                () -> solidClient.parseMembers("BAD", URI.create("http://localhost:3000/test/"))
        );
        assertEquals("Bad container listing", exception.getMessage());
    }

    @Test
    void deleteResource() {
        final String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test>.";
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);

        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        final SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteResourceRecursively(URI.create("http://localhost:3000/test")));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContents() throws IOException, InterruptedException {
        final String data = PREFIX + "<http://localhost:3000/> ldp:contains " +
                "<http://localhost:3000/test>, <http://localhost:3000/test2>.";
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);

        when(mockClient.getAsTurtle(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test2")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        final SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteContentsRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsTurtle(URI.create("http://localhost:3000/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test2"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContentsOneFails() throws IOException, InterruptedException {
        final String data = PREFIX + "<http://localhost:3000/> ldp:contains " +
                "<http://localhost:3000/test>, <http://localhost:3000/test2>.";
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);
        final HttpResponse<Void> mockResponseFail = TestUtils.mockVoidResponse(400);

        when(mockClient.getAsTurtle(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test2")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseFail));

        final SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteContentsRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsTurtle(URI.create("http://localhost:3000/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test2"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainerOneException() throws IOException, InterruptedException {
        final String data = PREFIX + "<http://localhost:3000/> ldp:contains " +
                "<http://localhost:3000/test>, <http://localhost:3000/test2>.";
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);
        final HttpResponse<Void> mockResponseException = mock(HttpResponse.class);
        // TODO: This causes a failure in a delete but the code cannot detect which so carries on deleting other
        // resources which may fail. Better handling needed.
        when(mockResponseException.statusCode()).thenThrow(new RuntimeException("FAIL"));

        when(mockClient.getAsTurtle(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test2")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseException));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        final SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteResourceRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsTurtle(URI.create("http://localhost:3000/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test2"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainerListFails() throws IOException, InterruptedException {
        final String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test>.";
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(400, null);

        when(mockClient.getAsTurtle(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);

        final SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteResourceRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsTurtle(URI.create("http://localhost:3000/"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainer() throws IOException, InterruptedException {
        final String data = PREFIX + "<http://localhost:3000/> ldp:contains " +
                "<http://localhost:3000/test>, <http://localhost:3000/test2>.";
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);

        when(mockClient.getAsTurtle(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test2")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        final SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteResourceRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsTurtle(URI.create("http://localhost:3000/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test2"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainerDeep() throws IOException, InterruptedException {
        final String data = PREFIX + "<http://localhost:3000/> ldp:contains " +
                "<http://localhost:3000/test>, <http://localhost:3000/child/>.";
        final String data2 = PREFIX + "<http://localhost:3000/child/> ldp:contains " +
                "<http://localhost:3000/test2>, <http://localhost:3000/test3>.";
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<String> mockResponseChild = TestUtils.mockStringResponse(200, data2);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);

        when(mockClient.getAsTurtle(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);
        when(mockClient.getAsTurtle(eq(URI.create("http://localhost:3000/child/")))).thenReturn(mockResponseChild);
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test2")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test3"))).
                thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/child/")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        final SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteResourceRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsTurtle(URI.create("http://localhost:3000/"));
        verify(mockClient).getAsTurtle(URI.create("http://localhost:3000/child/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test2"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test3"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/child/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteNull() {
        final SolidClient solidClient = new SolidClient();
        assertThrows(IllegalArgumentException.class, () -> solidClient.deleteResourceRecursively(null));
    }

    @Test
    void testToString() {
        final SolidClient solidClient = new SolidClient();
        assertEquals("SolidClient: user=, accessToken=null", solidClient.toString());
    }

    @Test
    void testToStringNamed() {
        clientRegistry.register("toStringUser", new Client.Builder("toStringUser").build());
        final SolidClient solidClient = new SolidClient("toStringUser");
        solidClient.getClient().setAccessToken("ACCESS");
        assertEquals("SolidClient: user=toStringUser, accessToken=ACCESS", solidClient.toString());
    }
}
