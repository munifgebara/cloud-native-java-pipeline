package br.com.stella.api.controller;

import br.com.stella.api.dto.CategoryResponseDTO;
import br.com.stella.api.dto.CategorySummaryDTO;
import br.com.stella.api.dto.SemanticSearchItemDTO;
import br.com.stella.api.dto.DashboardSummaryDTO;
import br.com.stella.api.dto.ItemLoanResponseDTO;
import br.com.stella.api.dto.MainItemImageDTO;
import br.com.stella.api.dto.ImageAiRequestDTO;
import br.com.stella.api.dto.ImageAiResponseDTO;
import br.com.stella.api.dto.ItemInstanceHistoryDTO;
import br.com.stella.api.dto.ItemInstanceResponseDTO;
import br.com.stella.api.dto.ItemInstanceSummaryDTO;
import br.com.stella.api.dto.MainItemResponseDTO;
import br.com.stella.api.dto.MainItemSummaryDTO;
import br.com.stella.api.dto.StorageLocationResponseDTO;
import br.com.stella.api.dto.StorageLocationSummaryDTO;
import br.com.stella.api.dto.LoginResponseDTO;
import br.com.stella.api.dto.MeuPerfilResponseDTO;
import br.com.stella.api.dto.ItemMovementResponseDTO;
import br.com.stella.api.dto.PersonResponseDTO;
import br.com.stella.api.dto.PersonSummaryDTO;
import br.com.stella.api.dto.UserResponseDTO;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.service.CategoryService;
import br.com.stella.api.service.DashboardService;
import br.com.stella.api.service.ItemLoanService;
import br.com.stella.api.service.ItemInstanceService;
import br.com.stella.api.service.ImageAiService;
import br.com.stella.api.service.MainItemService;
import br.com.stella.api.service.KeycloakLoginService;
import br.com.stella.api.service.KeycloakUserService;
import br.com.stella.api.service.StorageLocationService;
import br.com.stella.api.service.ItemMovementService;
import br.com.stella.api.service.PersonService;
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
        CategoryService service = mock(CategoryService.class);
        CategoryController controller = new CategoryController(service);
        UUID id = UUID.randomUUID();
        var response = new CategoryResponseDTO(id, "Livros", null, "book", true);
        var resumo = new CategorySummaryDTO(id, "Livros", null, "book", true);

        when(service.create(null)).thenReturn(response);
        when(service.findResponseById(id)).thenReturn(response);
        when(service.listSummary()).thenReturn(List.of(resumo));
        when(service.findByName("liv")).thenReturn(List.of(resumo));
        when(service.update(eq(id), any())).thenReturn(response);
        when(service.listSummaryIncludingInactive()).thenReturn(List.of(resumo));
        when(service.listRevisions(id)).thenReturn(List.of());

        assertThat(controller.create(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.findById(id).getBody()).isEqualTo(response);
        assertThat(controller.listar().getBody()).containsExactly(resumo);
        assertThat(controller.findByName("liv").getBody()).containsExactly(resumo);
        assertThat(controller.update(id, null).getBody()).isEqualTo(response);
        assertThat(controller.delete(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.findAllIncludingInactive().getBody()).containsExactly(resumo);
        assertThat(controller.listPreviousVersions(id).getBody()).isEmpty();

        verify(service).deleteLogically(id);
    }

    @Test
    void deveDelegarEndpointsDePessoa() {
        PersonService service = mock(PersonService.class);
        PersonController controller = new PersonController(service);
        UUID id = UUID.randomUUID();
        var response = mock(PersonResponseDTO.class);
        var resumo = mock(PersonSummaryDTO.class);

        when(service.create(null)).thenReturn(response);
        when(service.findResponseById(id)).thenReturn(response);
        when(service.listSummary()).thenReturn(List.of(resumo));
        when(service.findByName("ana")).thenReturn(List.of(resumo));
        when(service.update(id, null)).thenReturn(response);
        when(service.listSummaryIncludingInactive()).thenReturn(List.of(resumo));
        when(service.listRevisions(id)).thenReturn(List.of());

        assertThat(controller.create(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.findById(id).getBody()).isEqualTo(response);
        assertThat(controller.listar().getBody()).containsExactly(resumo);
        assertThat(controller.findByName("ana").getBody()).containsExactly(resumo);
        assertThat(controller.update(id, null).getBody()).isEqualTo(response);
        assertThat(controller.delete(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.findAllIncludingInactive().getBody()).containsExactly(resumo);
        assertThat(controller.listPreviousVersions(id).getBody()).isEmpty();

        verify(service).deleteLogically(id);
    }

    @Test
    void deveDelegarEndpointsDeItemMestre() {
        MainItemService service = mock(MainItemService.class);
        ImageAiService imagemIaService = mock(ImageAiService.class);
        MainItemController controller = new MainItemController(service, imagemIaService);
        UUID id = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        var response = mock(MainItemResponseDTO.class);
        var resumo = mock(MainItemSummaryDTO.class);
        var resultadoSemantico = mock(SemanticSearchItemDTO.class);
        var arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1});
        var imagemIaRequest = new ImageAiRequestDTO("Livro", "Livros", "Capa azul");
        var imagemIaResponse = new ImageAiResponseDTO("data:image/png;base64,abc", "image/png", "openai");

        when(service.create(null)).thenReturn(response);
        when(service.findResponseById(id)).thenReturn(response);
        when(service.listSummary()).thenReturn(List.of(resumo));
        when(service.findByName("livro")).thenReturn(List.of(resumo));
        when(service.filtrar("livro", categoriaId)).thenReturn(List.of(resumo));
        when(service.buscarSemanticamente("where is book")).thenReturn(List.of(resultadoSemantico));
        when(service.reindexarBuscaSemantica()).thenReturn(2);
        when(service.atualizarImagemPrincipal(id, arquivo, true, "openai")).thenReturn(response);
        when(imagemIaService.generateImage(imagemIaRequest)).thenReturn(imagemIaResponse);
        when(service.update(id, null)).thenReturn(response);
        when(service.listSummaryIncludingInactive()).thenReturn(List.of(resumo));
        when(service.listRevisions(id)).thenReturn(List.of());

        assertThat(controller.create(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.findById(id).getBody()).isEqualTo(response);
        assertThat(controller.listar().getBody()).containsExactly(resumo);
        assertThat(controller.findByName("livro").getBody()).containsExactly(resumo);
        assertThat(controller.filtrar("livro", categoriaId).getBody()).containsExactly(resumo);
        assertThat(controller.buscarSemanticamente("where is book").getBody()).containsExactly(resultadoSemantico);
        assertThat(controller.reindexarBuscaSemantica().getBody()).containsEntry("itensReindexados", 2);
        assertThat(controller.atualizarImagemPrincipal(id, arquivo, true, "openai").getBody()).isEqualTo(response);
        assertThat(controller.gerarImagemIa(imagemIaRequest).getBody()).isEqualTo(imagemIaResponse);
        assertThat(controller.update(id, null).getBody()).isEqualTo(response);
        assertThat(controller.delete(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.findAllIncludingInactive().getBody()).containsExactly(resumo);
        assertThat(controller.listPreviousVersions(id).getBody()).isEmpty();
    }

    @Test
    void deveDelegarEndpointsDeLocalArmazenamento() {
        StorageLocationService service = mock(StorageLocationService.class);
        StorageLocationController controller = new StorageLocationController(service);
        UUID id = UUID.randomUUID();
        var response = mock(StorageLocationResponseDTO.class);
        var resumo = mock(StorageLocationSummaryDTO.class);
        var arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1});

        when(service.create(null)).thenReturn(response);
        when(service.findResponseById(id)).thenReturn(response);
        when(service.listSummary()).thenReturn(List.of(resumo));
        when(service.findByName("dep")).thenReturn(List.of(resumo));
        when(service.updateImage(id, arquivo)).thenReturn(response);
        when(service.removerImagem(id)).thenReturn(response);
        when(service.update(id, null)).thenReturn(response);
        when(service.listSummaryIncludingInactive()).thenReturn(List.of(resumo));
        when(service.listRevisions(id)).thenReturn(List.of());

        assertThat(controller.create(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.findById(id).getBody()).isEqualTo(response);
        assertThat(controller.listar().getBody()).containsExactly(resumo);
        assertThat(controller.findByName("dep").getBody()).containsExactly(resumo);
        assertThat(controller.updateImage(id, arquivo).getBody()).isEqualTo(response);
        assertThat(controller.removerImagem(id).getBody()).isEqualTo(response);
        assertThat(controller.update(id, null).getBody()).isEqualTo(response);
        assertThat(controller.delete(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.findAllIncludingInactive().getBody()).containsExactly(resumo);
        assertThat(controller.listPreviousVersions(id).getBody()).isEmpty();
    }

    @Test
    void deveDelegarEndpointsDeInstanciaItem() {
        ItemInstanceService service = mock(ItemInstanceService.class);
        ItemInstanceController controller = new ItemInstanceController(service);
        UUID id = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        var response = mock(ItemInstanceResponseDTO.class);
        var resumo = mock(ItemInstanceSummaryDTO.class);
        var historico = mock(ItemInstanceHistoryDTO.class);

        when(service.create(null)).thenReturn(response);
        when(service.findResponseById(id)).thenReturn(response);
        when(service.buscarHistorico(id)).thenReturn(historico);
        when(service.listSummary()).thenReturn(List.of(resumo));
        when(service.findByIdentifier("pat")).thenReturn(List.of(resumo));
        when(service.filtrar("pat", "livro", categoriaId, ItemInstanceStatus.DISPONIVEL)).thenReturn(List.of(resumo));
        when(service.update(id, null)).thenReturn(response);
        when(service.listSummaryIncludingInactive()).thenReturn(List.of(resumo));
        when(service.listRevisions(id)).thenReturn(List.of());

        assertThat(controller.create(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.findById(id).getBody()).isEqualTo(response);
        assertThat(controller.buscarHistorico(id).getBody()).isEqualTo(historico);
        assertThat(controller.listar().getBody()).containsExactly(resumo);
        assertThat(controller.findByIdentifier("pat").getBody()).containsExactly(resumo);
        assertThat(controller.filtrar("pat", "livro", categoriaId, ItemInstanceStatus.DISPONIVEL).getBody()).containsExactly(resumo);
        assertThat(controller.update(id, null).getBody()).isEqualTo(response);
        assertThat(controller.delete(id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.findAllIncludingInactive().getBody()).containsExactly(resumo);
        assertThat(controller.listPreviousVersions(id).getBody()).isEmpty();
    }

    @Test
    void deveDelegarEndpointsOperacionais() {
        ItemMovementService movimentacaoService = mock(ItemMovementService.class);
        ItemLoanService emprestimoService = mock(ItemLoanService.class);
        var movimentacao = mock(ItemMovementResponseDTO.class);
        var emprestimo = mock(ItemLoanResponseDTO.class);

        when(movimentacaoService.registerInbound(null)).thenReturn(movimentacao);
        when(movimentacaoService.registerOutbound(null)).thenReturn(movimentacao);
        when(movimentacaoService.registerTransfer(null)).thenReturn(movimentacao);
        when(emprestimoService.registerLoan(null)).thenReturn(emprestimo);
        when(emprestimoService.registerReturn(null)).thenReturn(emprestimo);

        ItemMovementController movimentacoes = new ItemMovementController(movimentacaoService);
        ItemLoanController emprestimos = new ItemLoanController(emprestimoService);

        assertThat(movimentacoes.registerInbound(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(movimentacoes.registerOutbound(null).getBody()).isEqualTo(movimentacao);
        assertThat(movimentacoes.registerTransfer(null).getBody()).isEqualTo(movimentacao);
        assertThat(emprestimos.registerLoan(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(emprestimos.registerReturn(null).getBody()).isEqualTo(emprestimo);
    }

    @Test
    void deveDelegarEndpointsDeUsuarioEPerfil() {
        KeycloakUserService service = mock(KeycloakUserService.class);
        UserController controller = new UserController(service);
        Jwt jwt = jwt("user-1");
        var usuario = mock(UserResponseDTO.class);
        var perfil = mock(MeuPerfilResponseDTO.class);

        when(service.listar()).thenReturn(List.of(usuario));
        when(service.findById("user-1")).thenReturn(usuario);
        when(service.create(null)).thenReturn(usuario);
        when(service.update("user-1", null)).thenReturn(usuario);
        when(service.meuPerfil(jwt)).thenReturn(perfil);
        when(service.atualizarMeuPerfil(jwt, null)).thenReturn(perfil);

        assertThat(controller.listar().getBody()).containsExactly(usuario);
        assertThat(controller.findById("user-1").getBody()).isEqualTo(usuario);
        assertThat(controller.create(null).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.update("user-1", null).getBody()).isEqualTo(usuario);
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
        var resumo = mock(DashboardSummaryDTO.class);
        var login = new LoginResponseDTO("access", "refresh", "Bearer", 300L);

        when(dashboardService.carregarResumo()).thenReturn(resumo);
        when(loginService.login(null)).thenReturn(login);

        Map<String, Object> me = new AuthController().me(jwt("user-1"));

        assertThat(me).containsEntry("subject", "user-1");
        assertThat(me).containsEntry("username", "usuario");
        assertThat(new DashboardController(dashboardService).resumo()).isEqualTo(resumo);
        assertThat(new HomeController().test()).contains("Stella API");
        assertThat(new PublicAuthController(loginService).login(null)).isEqualTo(login);
    }

    @Test
    void deveResponderImagensPublicasComHeaders() {
        MainItemService itemService = mock(MainItemService.class);
        StorageLocationService localService = mock(StorageLocationService.class);
        UUID id = UUID.randomUUID();
        var imagem = new MainItemImageDTO("bucket", "objeto.png", "image/png", 2L);

        when(itemService.buscarMetadadosImagemPrincipal(id)).thenReturn(imagem);
        when(itemService.abrirImagemPrincipal(id)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2}));
        when(localService.buscarMetadadosImagem(id)).thenReturn(imagem);
        when(localService.abrirImagem(id)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2}));

        var itemResponse = new PublicMainItemImageController(itemService).buscarImagemPrincipal(id);
        var localResponse = new PublicStorageLocationImageController(localService).buscarImagem(id);

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
                .claim("name", "User Stella")
                .claim("email", "usuario@example.location")
                .claim("realm_access", Map.of("roles", List.of("usuario")))
                .issuer("http://keycloak/realms/stella")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
