package br.com.munif.pagadoria.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/api/test")
    public String test() {
        return "API Pagadoria protegida com sucesso.";
    }
}