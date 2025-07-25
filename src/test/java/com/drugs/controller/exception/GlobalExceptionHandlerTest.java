package com.drugs.controller.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class GlobalExceptionHandlerTest {

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    private MockMvc mockMvc;


    @Test
    public void testHandleDrugNotFound() {
        // Arrange
        DrugNotFoundException exception = new DrugNotFoundException("Drug not found");
        ResponseEntity<String> expectedResponse = new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);

        // Initialize mockMvc
        mockMvc = MockMvcBuilders.standaloneSetup(globalExceptionHandler).build();

        // Act
        ResponseEntity<String> actualResponse = globalExceptionHandler.handleDrugNotFound(exception);

        // Assert
        assertEquals(expectedResponse.getStatusCode(), actualResponse.getStatusCode());
        assertEquals(expectedResponse.getBody(), actualResponse.getBody());
    }

    @Test
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