package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.AlterarSenhaDTO;
import br.com.munif.stella.api.dto.MeuPerfilResponseDTO;
import br.com.munif.stella.api.dto.MeuPerfilUpdateDTO;
import br.com.munif.stella.api.dto.UsuarioCreateDTO;
import br.com.munif.stella.api.dto.UsuarioResponseDTO;
import br.com.munif.stella.api.dto.UsuarioUpdateDTO;
import br.com.munif.stella.api.service.KeycloakUsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v0/usuarios")
public class UsuarioController {

    private final KeycloakUsuarioService service;

    public UsuarioController(KeycloakUsuarioService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UsuarioResponseDTO> criar(@RequestBody @Valid UsuarioCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@PathVariable String id, @RequestBody @Valid UsuarioUpdateDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Void> alterarStatus(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        service.alterarStatus(id, Boolean.TRUE.equals(body.get("enabled")));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeuPerfilResponseDTO> meuPerfil(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.meuPerfil(jwt));
    }

    @PutMapping("/me")
    public ResponseEntity<MeuPerfilResponseDTO> atualizarMeuPerfil(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid MeuPerfilUpdateDTO dto
    ) {
        return ResponseEntity.ok(service.atualizarMeuPerfil(jwt, dto));
    }

    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarMinhaSenha(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AlterarSenhaDTO dto
    ) {
        service.alterarMinhaSenha(jwt, dto);
        return ResponseEntity.noContent().build();
    }
}
