package br.com.stella.api.service;

import br.com.stella.api.dto.StorageLocationCreateDTO;
import br.com.stella.api.dto.ImagemItemMestreDTO;
import br.com.stella.api.dto.StorageLocationUpdateDTO;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.repository.StorageLocationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LocalArmazenamentoServiceTest {

    @Mock
    private StorageLocationRepository repository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private MainItemImageStorageService imagemStorageService;

    @InjectMocks
    private StorageLocationService service;

    @Test
    void deveCriarLocalRaizNormalizandoCampos() {
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new StorageLocationCreateDTO("  Casa  ", "  Local principal  ", null, true));

        ArgumentCaptor<StorageLocation> captor = ArgumentCaptor.forClass(StorageLocation.class);
        verify(repository).save(captor.capture());

        StorageLocation localSalvo = captor.getValue();
        assertThat(localSalvo.getName()).isEqualTo("Casa");
        assertThat(localSalvo.getDescription()).isEqualTo("Local principal");
        assertThat(localSalvo.getParent()).isNull();
        assertThat(resposta.nome()).isEqualTo("Casa");
    }

    @Test
    void deveCriarSublocalComLocalPai() {
        UUID paiId = UUID.randomUUID();
        StorageLocation pai = location(paiId, "Escritorio", null);

        when(repository.findById(paiId)).thenReturn(Optional.of(pai));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.criar(new StorageLocationCreateDTO("Armario 1", null, paiId, true));

        ArgumentCaptor<StorageLocation> captor = ArgumentCaptor.forClass(StorageLocation.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getParent()).isEqualTo(pai);
    }

    @Test
    void deveCriarLocalInativoAposPersistenciaInicial() {
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new StorageLocationCreateDTO("Arquivo morto", null, null, false));

        assertThat(resposta.ativa()).isFalse();
        verify(repository).flush();
    }

    @Test
    void deveListarPreservandoHierarquia() {
        StorageLocation casa = location(UUID.randomUUID(), "Casa", null);
        StorageLocation escritorio = location(UUID.randomUUID(), "Escritorio", casa);
        StorageLocation gaveta = location(UUID.randomUUID(), "Gaveta 2", escritorio);
        StorageLocation deposito = location(UUID.randomUUID(), "Deposito", null);

        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(deposito, gaveta, escritorio, casa));

        var locais = service.listarResumo();

        assertThat(locais).extracting("nome").containsExactly("Casa", "Escritorio", "Gaveta 2", "Deposito");
        assertThat(locais).extracting("caminho").containsExactly(
                "Casa",
                "Casa > Escritorio",
                "Casa > Escritorio > Gaveta 2",
                "Deposito"
        );
        assertThat(locais).extracting("nivel").containsExactly(0, 1, 2, 0);
    }

    @Test
    void deveListarResumoIncluindoInativosEOrfaosComoRaiz() {
        StorageLocation removido = location(UUID.randomUUID(), "Removido", null);
        removido.setActive(false);
        StorageLocation orfao = location(UUID.randomUUID(), "Orfao", location(UUID.randomUUID(), "Pai ausente", null));

        when(repository.findAllIncludingInactive()).thenReturn(List.of(orfao, removido));

        var locais = service.listarResumoIncluindoInativos();

        assertThat(locais).extracting("nome").containsExactly("Orfao", "Removido");
        assertThat(locais).extracting("nivel").containsExactly(0, 0);
        assertThat(locais.getLast().ativa()).isFalse();
    }

    @Test
    void deveBuscarPorNomeSomenteQuandoFiltroInformado() {
        StorageLocation location = location(UUID.randomUUID(), "Deposito Central", null);

        when(repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc("Deposito")).thenReturn(List.of(location));

        assertThat(service.buscarPorNome("  ")).isEmpty();
        assertThat(service.buscarPorNome(" Deposito ")).hasSize(1);
    }

    @Test
    void deveImpedirLocalComoPaiDeleMesmo() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Casa", null);

        when(repository.findById(id)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.atualizar(id, new StorageLocationUpdateDTO("Casa", null, id, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own parent");
    }

    @Test
    void deveImpedirPaiDescendenteDoProprioLocal() {
        UUID casaId = UUID.randomUUID();
        UUID escritorioId = UUID.randomUUID();
        UUID gavetaId = UUID.randomUUID();

        StorageLocation casa = location(casaId, "Casa", null);
        StorageLocation escritorio = location(escritorioId, "Escritorio", casa);
        StorageLocation gaveta = location(gavetaId, "Gaveta", escritorio);

        when(repository.findById(casaId)).thenReturn(Optional.of(casa));
        when(repository.findById(gavetaId)).thenReturn(Optional.of(gaveta));

        assertThatThrownBy(() -> service.atualizar(casaId, new StorageLocationUpdateDTO("Casa", null, gavetaId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descendant");
    }

    @Test
    void deveImpedirLocalPaiInativo() {
        UUID paiId = UUID.randomUUID();
        StorageLocation pai = location(paiId, "Arquivo morto", null);
        pai.setActive(false);

        when(repository.findById(paiId)).thenReturn(Optional.of(pai));

        assertThatThrownBy(() -> service.criar(new StorageLocationCreateDTO("Caixa", null, paiId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parent location must be active.");

        verify(repository, never()).save(any(StorageLocation.class));
    }

    @Test
    void deveAtualizarLocalComPaiAtivoNormalizandoCampos() {
        UUID id = UUID.randomUUID();
        UUID paiId = UUID.randomUUID();
        StorageLocation location = location(id, "Antigo", null);
        StorageLocation pai = location(paiId, "Sala", null);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(repository.findById(paiId)).thenReturn(Optional.of(pai));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(id, new StorageLocationUpdateDTO("  Armario  ", "  Documentos  ", paiId, false));

        assertThat(resposta.nome()).isEqualTo("Armario");
        assertThat(resposta.descricao()).isEqualTo("Documentos");
        assertThat(resposta.paiId()).isEqualTo(paiId);
        assertThat(resposta.ativa()).isFalse();
    }

    @Test
    void deveAtualizarImagemDoLocalRemovendoImagemAnterior() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);
        location.setImagemBucket("bucket-antigo");
        location.setImagemObjectKey("locais/%s/antiga.png".formatted(id));

        var arquivo = new org.springframework.mock.web.MockMultipartFile("arquivo", "nova.png", "image/png", new byte[]{1, 2});
        var imagem = new ImagemItemMestreDTO("bucket-novo", "locais/%s/nova.png".formatted(id), "image/png", 2L);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(imagemStorageService.armazenarLocal(id, arquivo)).thenReturn(imagem);
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizarImagem(id, arquivo);

        assertThat(resposta.imagemUrl()).isEqualTo("/api/public/locais/%s/imagem".formatted(id));
        assertThat(location.getImagemBucket()).isEqualTo("bucket-novo");
        assertThat(location.getImagemObjectKey()).isEqualTo("locais/%s/nova.png".formatted(id));
        assertThat(location.getImagemContentType()).isEqualTo("image/png");
        assertThat(location.getImagemTamanhoBytes()).isEqualTo(2L);
        verify(imagemStorageService).removerSilenciosamente("bucket-antigo", "locais/%s/antiga.png".formatted(id));
    }

    @Test
    void deveRemoverImagemDoLocal() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);
        location.setImagemBucket("bucket");
        location.setImagemObjectKey("locais/%s/foto.png".formatted(id));
        location.setImagemContentType("image/png");
        location.setImagemTamanhoBytes(2L);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.removerImagem(id);

        assertThat(resposta.imagemUrl()).isNull();
        assertThat(location.getImagemBucket()).isNull();
        assertThat(location.getImagemObjectKey()).isNull();
        assertThat(location.getImagemContentType()).isNull();
        assertThat(location.getImagemTamanhoBytes()).isNull();
        verify(imagemStorageService).removerSilenciosamente("bucket", "locais/%s/foto.png".formatted(id));
    }

    @Test
    void naoDeveRemoverObjetoQuandoLocalNaoPossuiImagem() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.removerImagem(id);

        verify(imagemStorageService).removerSilenciosamente(null, null);
        verify(imagemStorageService, never()).armazenarLocal(any(), any());
    }

    @Test
    void deveBuscarMetadadosEAbrirImagemDoLocal() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);
        location.setImagemBucket("stella-locais");
        location.setImagemObjectKey("locais/%s/foto.png".formatted(id));
        location.setImagemContentType("image/png");
        location.setImagemTamanhoBytes(2L);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(imagemStorageService.abrir("stella-locais", "locais/%s/foto.png".formatted(id)))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2}));

        var metadados = service.buscarMetadadosImagem(id);
        var imagem = service.abrirImagem(id);

        assertThat(metadados.contentType()).isEqualTo("image/png");
        assertThat(metadados.tamanhoBytes()).isEqualTo(2L);
        assertThat(imagem).hasSameContentAs(new ByteArrayInputStream(new byte[]{1, 2}));
    }

    @Test
    void deveRejeitarMetadadosQuandoLocalNaoPossuiImagem() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);

        when(repository.findById(id)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.buscarMetadadosImagem(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Location does not have an image.");
    }

    private StorageLocation location(UUID id, String nome, StorageLocation pai) {
        StorageLocation location = new StorageLocation();
        location.setId(id);
        location.setName(nome);
        location.setParent(pai);
        location.setActive(true);
        return location;
    }
}
