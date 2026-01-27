package com.firstaidkit.integration.e2e.base;

import com.firstaidkit.integration.base.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

public abstract class BaseE2ETest extends AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    // Health check test removed - requires actuator dependency configuration

    protected ResponseEntity<String> postJson(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(path, entity, String.class);
    }

    protected ResponseEntity<Void> putJson(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(path, HttpMethod.PUT, entity, Void.class);
    }

    protected <T> ResponseEntity<T> getJson(String path, Class<T> responseType) {
        return restTemplate.getForEntity(path, responseType);
    }

    protected ResponseEntity<Void> delete(String path) {
        return restTemplate.exchange(path, HttpMethod.DELETE, null, Void.class);
    }

    protected ResponseEntity<byte[]> getBytes(String path) {
        return restTemplate.getForEntity(path, byte[].class);
    }
}