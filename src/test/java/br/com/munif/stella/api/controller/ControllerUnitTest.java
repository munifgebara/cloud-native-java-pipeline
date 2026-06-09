package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.CategoriaResponseDTO;
import br.com.munif.stella.api.dto.CategoriaResumoDTO;
import br.com.munif.stella.api.dto.DashboardResumoDTO;
import br.com.munif.stella.api.dto.EmprestimoItemResponseDTO;
import br.com.munif.stella.api.dto.ImagemItemMestreDTO;
import br.com.munif.stella.api.dto.InstanciaItemHistoricoDTO;
import br.com.munif.stella.api.dto.InstanciaItemResponseDTO;
import br.com.munif.stella.api.dto.InstanciaItemResumoDTO;
import br.com.munif.stella.api.dto.ItemMestreResponseDTO;
import br.com.munif.stella.api.dto.ItemMestreResumoDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResponseDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResumoDTO;
import br.com.munif.stella.api.dto.LoginResponseDTO;
import br.com.munif.stella.api.dto.MeuPerfilResponseDTO;
import br.com.munif.stella.api.dto.MovimentacaoItemResponseDTO;
import br.com.munif.stella.api.dto.PessoaResponseDTO;
import br.com.munif.stella.api.dto.PessoaResumoDTO;
import br.com.munif.stella.api.dto.UsuarioResponseDTO;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.service.CategoriaService;
import br.com.munif.stella.api.service.DashboardService;
import br.com.munif.stella.api.service.EmprestimoItemService;
import br.com.munif.stella.api.service.InstanciaItemService;
import br.com.munif.stella.api.service.ItemMestreService;
import br.com.munif.stella.api.service.KeycloakLoginService;
import br.com.munif.stella.api.service.KeycloakUsuarioService;
import br.com.munif.stella.api.service.LocalArmazenamentoService;
import br.com.munif.stella.api.service.MovimentacaoItemService;
import br.com.munif.stella.api.service.PessoaService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControllerUnitTest {

    @Test
    void deveDelegarEndpointsDeCategoria() {
        CategoriaService service = mock(CategoriaService.class);
        CategoriaController controller = new CategoriaController(service);
        UUID id = UUID.randomUUID();
        var response = new CategoriaResponseDTO(id, "Livros", null, "book", true);
        var resumo = new CategoriaResumoDTO(id, "Livros", null, "book", true);

        when(service.criar(null)).thenReturn(response);
        when(service.buscarResponsePorId(id)).thenReturn(response);
        when(service.listarResumo()).thenReturn(List.of(resumo));
        when(service.buscarPorNome("liv")).thenReturn(List.of(resumo));
        when(service.atualizar(eq(id), any())).thenReturn(response);
        when(service.listarResumoIncluindoInativos()).thenReturn(List.of(resumo));
        when(service.listarRevisoes(id)).thenReturn(List.of());

        assertThat(controller.criar(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.buscarPorId(id).getBody()).isEqualTo(response);
        assertThat(controller.listar().getBody()).containsExactly(resumo);
        assertThat(controller.buscarPorNome("liv").getBody()).containsExactly(resumo);
        assertThat(controller.atualizar(id, null).getBody()).isEqualTo(response);
        assertThat(controller.excluir(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.listarTodosIncluindoInativos().getBody()).containsExactly(resumo);
        assertThat(controller.listarVersoesAnteriores(id).getBody()).isEmpty();

        verify(service).excluirLogicamente(id);
    }

    @Test
    void deveDelegarEndpointsDePessoa() {
        PessoaService service = mock(PessoaService.class);
        PessoaController controller = new PessoaController(service);
        UUID id = UUID.randomUUID();
        var response = mock(PessoaResponseDTO.class);
        var resumo = mock(PessoaResumoDTO.class);

        when(service.criar(null)).thenReturn(response);
        when(service.buscarResponsePorId(id)).thenReturn(response);
        when(service.listarResumo()).thenReturn(List.of(resumo));
        when(service.buscarPorNome("ana")).thenReturn(List.of(resumo));
        when(service.atualizar(id, null)).thenReturn(response);
        when(service.listarResumoIncluindoInativos()).thenReturn(List.of(resumo));
        when(service.listarRevisoes(id)).thenReturn(List.of());

        assertThat(controller.criar(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.buscarPorId(id).getBody()).isEqualTo(response);
        assertThat(controller.listar().getBody()).containsExactly(resumo);
        assertThat(controller.buscarPorNome("ana").getBody()).containsExactly(resumo);
        assertThat(controller.atualizar(id, null).getBody()).isEqualTo(response);
        assertThat(controller.excluir(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.listarTodosIncluindoInativos().getBody()).containsExactly(resumo);
        assertThat(controller.listarVersoesAnteriores(id).getBody()).isEmpty();

        verify(service).excluirLogicamente(id);
    }

    @Test
    void deveDelegarEndpointsDeItemMestre() {
        ItemMestreService service = mock(ItemMestreService.class);
        ItemMestreController controller = new ItemMestreController(service);
        UUID id = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        var response = mock(ItemMestreResponseDTO.class);
        var resumo = mock(ItemMestreResumoDTO.class);
        var arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1});

        when(service.criar(null)).thenReturn(response);
        when(service.buscarResponsePorId(id)).thenReturn(response);
        when(service.listarResumo()).thenReturn(List.of(resumo));
        when(service.buscarPorNome("livro")).thenReturn(List.of(resumo));
        when(service.filtrar("livro", categoriaId)).thenReturn(List.of(resumo));
        when(service.atualizarImagemPrincipal(id, arquivo)).thenReturn(response);
        when(service.atualizar(id, null)).thenReturn(response);
        when(service.listarResumoIncluindoInativos()).thenReturn(List.of(resumo));
        when(service.listarRevisoes(id)).thenReturn(List.of());

        assertThat(controller.criar(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.buscarPorId(id).getBody()).isEqualTo(response);
        assertThat(controller.listar().getBody()).containsExactly(resumo);
        assertThat(controller.buscarPorNome("livro").getBody()).containsExactly(resumo);
        assertThat(controller.filtrar("livro", categoriaId).getBody()).containsExactly(resumo);
        assertThat(controller.atualizarImagemPrincipal(id, arquivo).getBody()).isEqualTo(response);
        assertThat(controller.atualizar(id, null).getBody()).isEqualTo(response);
        assertThat(controller.excluir(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.listarTodosIncluindoInativos().getBody()).containsExactly(resumo);
        assertThat(controller.listarVersoesAnteriores(id).getBody()).isEmpty();
    }

    @Test
    void deveDelegarEndpointsDeLocalArmazenamento() {
        LocalArmazenamentoService service = mock(LocalArmazenamentoService.class);
        LocalArmazenamentoController controller = new LocalArmazenamentoController(service);
        UUID id = UUID.randomUUID();
        var response = mock(LocalArmazenamentoResponseDTO.class);
        var resumo = mock(LocalArmazenamentoResumoDTO.class);
        var arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1});

        when(service.criar(null)).thenReturn(response);
        when(service.buscarResponsePorId(id)).thenReturn(response);
        when(service.listarResumo()).thenReturn(List.of(resumo));
        when(service.buscarPorNome("dep")).thenReturn(List.of(resumo));
        when(service.atualizarImagem(id, arquivo)).thenReturn(response);
        when(service.removerImagem(id)).thenReturn(response);
        when(service.atualizar(id, null)).thenReturn(response);
        when(service.listarResumoIncluindoInativos()).thenReturn(List.of(resumo));
        when(service.listarRevisoes(id)).thenReturn(List.of());

        assertThat(controller.criar(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.buscarPorId(id).getBody()).isEqualTo(response);
        assertThat(controller.listar().getBody()).containsExactly(resumo);
        assertThat(controller.buscarPorNome("dep").getBody()).containsExactly(resumo);
        assertThat(controller.atualizarImagem(id, arquivo).getBody()).isEqualTo(response);
        assertThat(controller.removerImagem(id).getBody()).isEqualTo(response);
        assertThat(controller.atualizar(id, null).getBody()).isEqualTo(response);
        assertThat(controller.excluir(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.listarTodosIncluindoInativos().getBody()).containsExactly(resumo);
        assertThat(controller.listarVersoesAnteriores(id).getBody()).isEmpty();
    }

    @Test
    void deveDelegarEndpointsDeInstanciaItem() {
        InstanciaItemService service = mock(InstanciaItemService.class);
        InstanciaItemController controller = new InstanciaItemController(service);
        UUID id = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        var response = mock(InstanciaItemResponseDTO.class);
        var resumo = mock(InstanciaItemResumoDTO.class);
        var historico = mock(InstanciaItemHistoricoDTO.class);

        when(service.criar(null)).thenReturn(response);
        when(service.buscarResponsePorId(id)).thenReturn(response);
        when(service.buscarHistorico(id)).thenReturn(historico);
        when(service.listarResumo()).thenReturn(List.of(resumo));
        when(service.buscarPorIdentificador("pat")).thenReturn(List.of(resumo));
        when(service.filtrar("pat", "livro", categoriaId, StatusOperacionalInstancia.DISPONIVEL)).thenReturn(List.of(resumo));
        when(service.atualizar(id, null)).thenReturn(response);
        when(service.listarResumoIncluindoInativos()).thenReturn(List.of(resumo));
        when(service.listarRevisoes(id)).thenReturn(List.of());

        assertThat(controller.criar(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.buscarPorId(id).getBody()).isEqualTo(response);
        assertThat(controller.buscarHistorico(id).getBody()).isEqualTo(historico);
        assertThat(controller.listar().getBody()).containsExactly(resumo);
        assertThat(controller.buscarPorIdentificador("pat").getBody()).containsExactly(resumo);
        assertThat(controller.filtrar("pat", "livro", categoriaId, StatusOperacionalInstancia.DISPONIVEL).getBody()).containsExactly(resumo);
        assertThat(controller.atualizar(id, null).getBody()).isEqualTo(response);
        assertThat(controller.excluir(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.listarTodosIncluindoInativos().getBody()).containsExactly(resumo);
        assertThat(controller.listarVersoesAnteriores(id).getBody()).isEmpty();
    }

    @Test
    void deveDelegarEndpointsOperacionais() {
        MovimentacaoItemService movimentacaoService = mock(MovimentacaoItemService.class);
        EmprestimoItemService emprestimoService = mock(EmprestimoItemService.class);
        var movimentacao = mock(MovimentacaoItemResponseDTO.class);
        var emprestimo = mock(EmprestimoItemResponseDTO.class);

        when(movimentacaoService.registrarEntrada(null)).thenReturn(movimentacao);
        when(movimentacaoService.registrarSaida(null)).thenReturn(movimentacao);
        when(movimentacaoService.registrarTransferencia(null)).thenReturn(movimentacao);
        when(emprestimoService.registrarEmprestimo(null)).thenReturn(emprestimo);
        when(emprestimoService.registrarDevolucao(null)).thenReturn(emprestimo);

        MovimentacaoItemController movimentacoes = new MovimentacaoItemController(movimentacaoService);
        EmprestimoItemController emprestimos = new EmprestimoItemController(emprestimoService);

        assertThat(movimentacoes.registrarEntrada(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(movimentacoes.registrarSaida(null).getBody()).isEqualTo(movimentacao);
        assertThat(movimentacoes.registrarTransferencia(null).getBody()).isEqualTo(movimentacao);
        assertThat(emprestimos.registrarEmprestimo(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(emprestimos.registrarDevolucao(null).getBody()).isEqualTo(emprestimo);
    }

    @Test
    void deveDelegarEndpointsDeUsuarioEPerfil() {
        KeycloakUsuarioService service = mock(KeycloakUsuarioService.class);
        UsuarioController controller = new UsuarioController(service);
        Jwt jwt = jwt("user-1");
        var usuario = mock(UsuarioResponseDTO.class);
        var perfil = mock(MeuPerfilResponseDTO.class);

        when(service.listar()).thenReturn(List.of(usuario));
        when(service.buscarPorId("user-1")).thenReturn(usuario);
        when(service.criar(null)).thenReturn(usuario);
        when(service.atualizar("user-1", null)).thenReturn(usuario);
        when(service.meuPerfil(jwt)).thenReturn(perfil);
        when(service.atualizarMeuPerfil(jwt, null)).thenReturn(perfil);

        assertThat(controller.listar().getBody()).containsExactly(usuario);
        assertThat(controller.buscarPorId("user-1").getBody()).isEqualTo(usuario);
        assertThat(controller.criar(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.atualizar("user-1", null).getBody()).isEqualTo(usuario);
        assertThat(controller.alterarStatus("user-1", Map.of("enabled", true)).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.meuPerfil(jwt).getBody()).isEqualTo(perfil);
        assertThat(controller.atualizarMeuPerfil(jwt, null).getBody()).isEqualTo(perfil);
        assertThat(controller.alterarMinhaSenha(jwt, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(service).alterarStatus("user-1", true);
        verify(service).alterarMinhaSenha(jwt, null);
    }

    @Test
    void deveMontarRespostaDeAutenticacaoDashboardHomeELogin() {
        DashboardService dashboardService = mock(DashboardService.class);
        KeycloakLoginService loginService = mock(KeycloakLoginService.class);
        var resumo = mock(DashboardResumoDTO.class);
        var login = new LoginResponseDTO("access", "refresh", "Bearer", 300L);

        when(dashboardService.carregarResumo()).thenReturn(resumo);
        when(loginService.login(null)).thenReturn(login);

        Map<String, Object> me = new AuthController().me(jwt("user-1"));

        assertThat(me).containsEntry("subject", "user-1");
        assertThat(me).containsEntry("username", "usuario");
        assertThat(new DashboardController(dashboardService).resumo()).isEqualTo(resumo);
        assertThat(new HomeController().test()).contains("API Stella");
        assertThat(new PublicAuthController(loginService).login(null)).isEqualTo(login);
    }

    @Test
    void deveResponderImagensPublicasComHeaders() {
        ItemMestreService itemService = mock(ItemMestreService.class);
        LocalArmazenamentoService localService = mock(LocalArmazenamentoService.class);
        UUID id = UUID.randomUUID();
        var imagem = new ImagemItemMestreDTO("bucket", "objeto.png", "image/png", 2L);

        when(itemService.buscarMetadadosImagemPrincipal(id)).thenReturn(imagem);
        when(itemService.abrirImagemPrincipal(id)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2}));
        when(localService.buscarMetadadosImagem(id)).thenReturn(imagem);
        when(localService.abrirImagem(id)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2}));

        var itemResponse = new PublicItemMestreImagemController(itemService).buscarImagemPrincipal(id);
        var localResponse = new PublicLocalArmazenamentoImagemController(localService).buscarImagem(id);

        assertThat(itemResponse.getBody()).isInstanceOf(InputStreamResource.class);
        assertThat(itemResponse.getHeaders().getContentType().toString()).isEqualTo("image/png");
        assertThat(itemResponse.getHeaders().getContentLength()).isEqualTo(2L);
        assertThat(localResponse.getBody()).isInstanceOf(InputStreamResource.class);
        assertThat(localResponse.getHeaders().getContentType().toString()).isEqualTo("image/png");
        assertThat(localResponse.getHeaders().getContentLength()).isEqualTo(2L);
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim("preferred_username", "usuario")
                .claim("name", "Usuario Stella")
                .claim("email", "usuario@example.local")
                .claim("realm_access", Map.of("roles", List.of("usuario")))
                .issuer("http://keycloak/realms/stella")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
