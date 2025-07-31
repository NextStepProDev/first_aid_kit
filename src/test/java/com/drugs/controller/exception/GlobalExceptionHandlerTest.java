package com.drugs.controller.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
@SpringBootTest
@ActiveProfiles("test")
public class GlobalExceptionHandlerTest {

    @Autowired
    @SuppressWarnings("unused")
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("Should return 404 and error message when drug is not found")
    public void testHandleDrugNotFound() {
        // Arrange & Act
        DrugNotFoundException exception = new DrugNotFoundException("Drug not found");
        ResponseEntity<ErrorMessage> actualResponse = globalExceptionHandler.handleDrugNotFound(exception);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, actualResponse.getStatusCode());
        assertNotNull(actualResponse.getBody());
        assertEquals(404, actualResponse.getBody().status());
        assertEquals("Drug not found", actualResponse.getBody().message());
        assertNotNull(actualResponse.getBody().timestamp());
    }

    @Test
    @DisplayName("Should return 500 and generic error message for unexpected exception")
    public void testHandleGeneralException() {
        // Tworzymy ogólny wyjątek
        Exception exception = new Exception("An unexpected error occurred");

        // Wywołanie metody handleGeneralException
        ResponseEntity<String> response = globalExceptionHandler.handleGeneralException(exception);

        // Sprawdzanie, czy odpowiedź ma poprawny status i treść
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An error occurred", response.getBody());
    }
}