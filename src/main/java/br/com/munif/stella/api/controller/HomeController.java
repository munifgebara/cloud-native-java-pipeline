package br.com.munif.stella.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auxiliary REST controller for checking API availability.
 *
 * <p>The {@code GET /api/test} endpoint confirms that the API is up and that
 * JWT authentication is functional, returning a simple message.</p>
 */
@RestController
public class HomeController {

    /**
     * Sanity endpoint to verify that the API is operational and authenticated.
     *
     * @return confirmation message
     */
    @GetMapping("/api/test")
    public String test() {
        return "Stella API successfully secured.";
    }
}