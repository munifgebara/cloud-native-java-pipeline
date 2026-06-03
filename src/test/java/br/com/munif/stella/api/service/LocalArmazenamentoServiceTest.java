package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.LocalArmazenamentoCreateDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoUpdateDTO;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalArmazenamentoServiceTest {

    @Mock
    private LocalArmazenamentoRepository repository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private LocalArmazenamentoService service;

    @Test
    void deveCriarLocalRaizNormalizandoCampos() {
        when(repository.save(any(LocalArmazenamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new LocalArmazenamentoCreateDTO("  Casa  ", "  Local principal  ", null, true));

        ArgumentCaptor<LocalArmazenamento> captor = ArgumentCaptor.forClass(LocalArmazenamento.class);
        verify(repository).save(captor.capture());

        LocalArmazenamento localSalvo = captor.getValue();
        assertThat(localSalvo.getNome()).isEqualTo("Casa");
        assertThat(localSalvo.getDescricao()).isEqualTo("Local principal");
        assertThat(localSalvo.getPai()).isNull();
        assertThat(resposta.nome()).isEqualTo("Casa");
    }

    @Test
    void deveCriarSublocalComLocalPai() {
        UUID paiId = UUID.randomUUID();
        LocalArmazenamento pai = local(paiId, "Escritorio", null);

        when(repository.findById(paiId)).thenReturn(Optional.of(pai));
        when(repository.save(any(LocalArmazenamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.criar(new LocalArmazenamentoCreateDTO("Armario 1", null, paiId, true));

        ArgumentCaptor<LocalArmazenamento> captor = ArgumentCaptor.forClass(LocalArmazenamento.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getPai()).isEqualTo(pai);
    }

    @Test
    void deveListarPreservandoHierarquia() {
        LocalArmazenamento casa = local(UUID.randomUUID(), "Casa", null);
        LocalArmazenamento escritorio = local(UUID.randomUUID(), "Escritorio", casa);
        LocalArmazenamento gaveta = local(UUID.randomUUID(), "Gaveta 2", escritorio);
        LocalArmazenamento deposito = local(UUID.randomUUID(), "Deposito", null);

        when(repository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(deposito, gaveta, escritorio, casa));

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
    void deveImpedirLocalComoPaiDeleMesmo() {
        UUID id = UUID.randomUUID();
        LocalArmazenamento local = local(id, "Casa", null);

        when(repository.findById(id)).thenReturn(Optional.of(local));

        assertThatThrownBy(() -> service.atualizar(id, new LocalArmazenamentoUpdateDTO("Casa", null, id, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pai dele mesmo");
    }

    @Test
    void deveImpedirPaiDescendenteDoProprioLocal() {
        UUID casaId = UUID.randomUUID();
        UUID escritorioId = UUID.randomUUID();
        UUID gavetaId = UUID.randomUUID();

        LocalArmazenamento casa = local(casaId, "Casa", null);
        LocalArmazenamento escritorio = local(escritorioId, "Escritorio", casa);
        LocalArmazenamento gaveta = local(gavetaId, "Gaveta", escritorio);

        when(repository.findById(casaId)).thenReturn(Optional.of(casa));
        when(repository.findById(gavetaId)).thenReturn(Optional.of(gaveta));

        assertThatThrownBy(() -> service.atualizar(casaId, new LocalArmazenamentoUpdateDTO("Casa", null, gavetaId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descendente");
    }

    private LocalArmazenamento local(UUID id, String nome, LocalArmazenamento pai) {
        LocalArmazenamento local = new LocalArmazenamento();
        local.setId(id);
        local.setNome(nome);
        local.setPai(pai);
        local.setAtivo(true);
        return local;
    }
}
