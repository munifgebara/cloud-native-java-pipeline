package br.com.munif.pagadoria.api.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class SpaForwardController {

    @GetMapping("/app")
    public String forwardApp() {
        return "forward:/app/index.html";
    }
}