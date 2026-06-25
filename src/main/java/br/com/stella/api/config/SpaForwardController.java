package br.com.stella.api.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class SpaForwardController {

    @GetMapping({
            "/app",
            "/app/",
            "/app/auth/callback"
    })
    public String forwardApp() {
        return "forward:/app/index.html";
    }
}
