package br.com.stella.api.service;

import br.com.stella.api.dto.StorageLocationCreateDTO;
import br.com.stella.api.dto.MainItemImageDTO;
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
    private MainItemImageStorageService imageStorageService;

    @InjectMocks
    private StorageLocationService service;

    @Test
    void deveCriarLocalRaizNormalizandoCampos() {
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new StorageLocationCreateDTO("  Casa  ", "  Local principal  ", null, true));

        ArgumentCaptor<StorageLocation> captor = ArgumentCaptor.forClass(StorageLocation.class);
        verify(repository).save(captor.capture());

        StorageLocation localSalvo = captor.getValue();
        assertThat(localSalvo.getName()).isEqualTo("Casa");
        assertThat(localSalvo.getDescription()).isEqualTo("Local principal");
        assertThat(localSalvo.getParent()).isNull();
        assertThat(response.name()).isEqualTo("Casa");
    }

    @Test
    void deveCriarSublocalComLocalPai() {
        UUID paiId = UUID.randomUUID();
        StorageLocation pai = location(paiId, "Escritorio", null);

        when(repository.findById(paiId)).thenReturn(Optional.of(pai));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new StorageLocationCreateDTO("Armario 1", null, paiId, true));

        ArgumentCaptor<StorageLocation> captor = ArgumentCaptor.forClass(StorageLocation.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getParent()).isEqualTo(pai);
    }

    @Test
    void deveCriarLocalInativoAposPersistenciaInicial() {
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new StorageLocationCreateDTO("Arquivo morto", null, null, false));

        assertThat(response.ativa()).isFalse();
        verify(repository).flush();
    }

    @Test
    void deveListarPreservandoHierarquia() {
        StorageLocation casa = location(UUID.randomUUID(), "Casa", null);
        StorageLocation escritorio = location(UUID.randomUUID(), "Escritorio", casa);
        StorageLocation gaveta = location(UUID.randomUUID(), "Gaveta 2", escritorio);
        StorageLocation deposito = location(UUID.randomUUID(), "Deposito", null);

        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(deposito, gaveta, escritorio, casa));

        var locais = service.listSummary();

        assertThat(locais).extracting("name").containsExactly("Casa", "Escritorio", "Gaveta 2", "Deposito");
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

        var locais = service.listSummaryIncludingInactive();

        assertThat(locais).extracting("name").containsExactly("Orfao", "Removido");
        assertThat(locais).extracting("nivel").containsExactly(0, 0);
        assertThat(locais.getLast().ativa()).isFalse();
    }

    @Test
    void deveBuscarPorNomeSomenteQuandoFiltroInformado() {
        StorageLocation location = location(UUID.randomUUID(), "Deposito Central", null);

        when(repository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc("Deposito")).thenReturn(List.of(location));

        assertThat(service.findByName("  ")).isEmpty();
        assertThat(service.findByName(" Deposito ")).hasSize(1);
    }

    @Test
    void deveImpedirLocalComoPaiDeleMesmo() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Casa", null);

        when(repository.findById(id)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.update(id, new StorageLocationUpdateDTO("Casa", null, id, true)))
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

        assertThatThrownBy(() -> service.update(casaId, new StorageLocationUpdateDTO("Casa", null, gavetaId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descendant");
    }

    @Test
    void deveImpedirLocalPaiInativo() {
        UUID paiId = UUID.randomUUID();
        StorageLocation pai = location(paiId, "Arquivo morto", null);
        pai.setActive(false);

        when(repository.findById(paiId)).thenReturn(Optional.of(pai));

        assertThatThrownBy(() -> service.create(new StorageLocationCreateDTO("Caixa", null, paiId, true)))
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

        var response = service.update(id, new StorageLocationUpdateDTO("  Armario  ", "  Documentos  ", paiId, false));

        assertThat(response.name()).isEqualTo("Armario");
        assertThat(response.description()).isEqualTo("Documentos");
        assertThat(response.paiId()).isEqualTo(paiId);
        assertThat(response.ativa()).isFalse();
    }

    @Test
    void deveAtualizarImagemDoLocalRemovendoImagemAnterior() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);
        location.setImageBucket("bucket-antigo");
        location.setImageObjectKey("locais/%s/antiga.png".formatted(id));

        var arquivo = new org.springframework.mock.web.MockMultipartFile("arquivo", "nova.png", "image/png", new byte[]{1, 2});
        var image = new MainItemImageDTO("bucket-novo", "locais/%s/nova.png".formatted(id), "image/png", 2L);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(imageStorageService.armazenarLocal(id, arquivo)).thenReturn(image);
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateImage(id, arquivo);

        assertThat(response.imageUrl()).isEqualTo("/api/public/locais/%s/image".formatted(id));
        assertThat(location.getImageBucket()).isEqualTo("bucket-novo");
        assertThat(location.getImageObjectKey()).isEqualTo("locais/%s/nova.png".formatted(id));
        assertThat(location.getImageContentType()).isEqualTo("image/png");
        assertThat(location.getImageSizeBytes()).isEqualTo(2L);
        verify(imageStorageService).removerSilenciosamente("bucket-antigo", "locais/%s/antiga.png".formatted(id));
    }

    @Test
    void deveRemoverImagemDoLocal() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);
        location.setImageBucket("bucket");
        location.setImageObjectKey("locais/%s/foto.png".formatted(id));
        location.setImageContentType("image/png");
        location.setImageSizeBytes(2L);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.removerImagem(id);

        assertThat(response.imageUrl()).isNull();
        assertThat(location.getImageBucket()).isNull();
        assertThat(location.getImageObjectKey()).isNull();
        assertThat(location.getImageContentType()).isNull();
        assertThat(location.getImageSizeBytes()).isNull();
        verify(imageStorageService).removerSilenciosamente("bucket", "locais/%s/foto.png".formatted(id));
    }

    @Test
    void naoDeveRemoverObjetoQuandoLocalNaoPossuiImagem() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.removerImagem(id);

        verify(imageStorageService).removerSilenciosamente(null, null);
        verify(imageStorageService, never()).armazenarLocal(any(), any());
    }

    @Test
    void deveBuscarMetadadosEAbrirImagemDoLocal() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);
        location.setImageBucket("stella-locais");
        location.setImageObjectKey("locais/%s/foto.png".formatted(id));
        location.setImageContentType("image/png");
        location.setImageSizeBytes(2L);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(imageStorageService.abrir("stella-locais", "locais/%s/foto.png".formatted(id)))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2}));

        var metadados = service.buscarMetadadosImagem(id);
        var image = service.abrirImagem(id);

        assertThat(metadados.contentType()).isEqualTo("image/png");
        assertThat(metadados.tamanhoBytes()).isEqualTo(2L);
        assertThat(image).hasSameContentAs(new ByteArrayInputStream(new byte[]{1, 2}));
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

    private StorageLocation location(UUID id, String name, StorageLocation pai) {
        StorageLocation location = new StorageLocation();
        location.setId(id);
        location.setName(name);
        location.setParent(pai);
        location.setActive(true);
        return location;
    }
}
