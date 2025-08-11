// src/test/java/com/firstaid/e2e/BaseE2ETest.java
package com.firstaid.integration.e2e.base;

import com.firstaid.integration.base.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public abstract class BaseE2ETest extends AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate rest;

    protected String url(String path) {
        if (path == null) return "http://localhost:" + port + "/";
        if (!path.startsWith("/")) path = "/" + path;
        return "http://localhost:" + port + path;
    }

    protected HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.add("Content-Type", "application/json");
        h.add("Accept", "application/json");
        return h;
    }

    protected <T> ResponseEntity<T> exchangeJson(HttpMethod method, String path, String body, Class<T> responseType) {
        HttpEntity<String> entity = new HttpEntity<>(body, jsonHeaders());
        return rest.exchange(url(path), method, entity, responseType);
    }

    protected <T> ResponseEntity<T> postJson(String path, String body, Class<T> responseType) {
        return exchangeJson(HttpMethod.POST, path, body, responseType);
    }

    protected <T> ResponseEntity<T> putJson(String path, String body, Class<T> responseType) {
        return exchangeJson(HttpMethod.PUT, path, body, responseType);
    }

    protected <T> ResponseEntity<T> getJson(String path, Class<T> responseType) {
        return rest.getForEntity(url(path), responseType);
    }

    protected ResponseEntity<Void> delete(String path) {
        return rest.exchange(url(path), HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
    }

    protected ResponseEntity<byte[]> getBytes(String path) {
        return rest.getForEntity(url(path), byte[].class);
    }
}