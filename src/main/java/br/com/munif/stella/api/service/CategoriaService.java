package br.com.munif.stella.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.CategoriaCreateDTO;
import br.com.munif.stella.api.dto.CategoriaResponseDTO;
import br.com.munif.stella.api.dto.CategoriaResumoDTO;
import br.com.munif.stella.api.dto.CategoriaUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.CategoriaIcone;
import br.com.munif.stella.api.mapper.CategoriaMapper;
import br.com.munif.stella.api.repository.CategoriaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pelas operações de negócio sobre {@link Categoria}.
 *
 * <p>Orquestra persistência, normalização de campos e consultas, delegando ao
 * {@link CategoriaRepository} e ao {@link CategoriaMapper} conforme necessário.</p>
 */
@Service
public class CategoriaService extends SuperService<Categoria, CategoriaRepository> {

    /**
     * Constrói o serviço injetando o repositório e o {@code EntityManager}.
     *
     * @param repository    repositório JPA de categorias
     * @param entityManager gerenciador de entidades, usado internamente pelo {@code SuperService}
     *                      para consultas Envers
     */
    public CategoriaService(CategoriaRepository repository, EntityManager entityManager) {
        super(repository, entityManager, Categoria.class);
    }

    /**
     * Cria uma nova categoria a partir do DTO de entrada.
     *
     * <p>Se o campo {@code ativa} do DTO for {@code false}, a categoria é criada ativa
     * (restrição do {@code @PrePersist}) e depois inativada em uma segunda operação.</p>
     *
     * @param dto dados de criação validados pelo Bean Validation
     * @return DTO completo da categoria criada
     * @throws IllegalArgumentException se o ícone fornecido for inválido
     */
    @Transactional
    public CategoriaResponseDTO criar(CategoriaCreateDTO dto) {
        Categoria categoria = CategoriaMapper.toEntity(dto);
        normalizarCampos(categoria);

        Categoria salva = salvar(categoria);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salva.setAtivo(false);
            salva = salvar(salva);
        }
        return CategoriaMapper.toResponseDTO(salva);
    }

    /**
     * Retorna o DTO completo de uma categoria pelo seu identificador.
     *
     * @param id UUID da categoria
     * @return DTO completo da categoria
     * @throws jakarta.persistence.EntityNotFoundException se a categoria não existir
     */
    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarResponsePorId(UUID id) {
        return CategoriaMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Lista todas as categorias ativas em ordem alfabética pelo nome.
     *
     * @return lista de DTOs de resumo das categorias ativas
     */
    @Transactional(readOnly = true)
    public List<CategoriaResumoDTO> listarResumo() {
        return repository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(CategoriaMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lista todas as categorias, incluindo as inativas.
     *
     * @return lista de DTOs de resumo de todas as categorias
     */
    @Transactional(readOnly = true)
    public List<CategoriaResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(CategoriaMapper::toResumoDTO)
                .toList();
    }

    /**
     * Busca categorias ativas cujo nome contenha o texto informado (busca parcial, sem distinção de maiúsculas).
     *
     * @param nome texto a buscar no nome da categoria; retorna lista vazia se em branco
     * @return lista de DTOs de resumo das categorias encontradas
     */
    @Transactional(readOnly = true)
    public List<CategoriaResumoDTO> buscarPorNome(String nome) {
        String nomeTratado = ValidacoesBR.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(nomeTratado).stream()
                .map(CategoriaMapper::toResumoDTO)
                .toList();
    }

    /**
     * Atualiza os dados de uma categoria existente.
     *
     * @param id  UUID da categoria a atualizar
     * @param dto dados de atualização validados pelo Bean Validation
     * @return DTO completo da categoria atualizada
     * @throws jakarta.persistence.EntityNotFoundException se a categoria não existir
     * @throws IllegalArgumentException se o ícone fornecido for inválido
     */
    @Transactional
    public CategoriaResponseDTO atualizar(UUID id, CategoriaUpdateDTO dto) {
        Categoria categoria = buscarPorId(id);
        CategoriaMapper.updateEntity(categoria, dto);
        normalizarCampos(categoria);

        Categoria salva = salvar(categoria);
        return CategoriaMapper.toResponseDTO(salva);
    }

    /**
     * Inativa logicamente uma categoria (define {@code ativo = false}).
     *
     * @param id UUID da categoria a inativar
     * @throws jakarta.persistence.EntityNotFoundException se a categoria não existir
     */
    @Transactional
    public void excluirLogicamente(UUID id) {
        excluir(id);
    }

    /**
     * Retorna o histórico de revisões anteriores de uma categoria (Hibernate Envers).
     *
     * @param id UUID da categoria
     * @return lista de revisões em ordem cronológica; lista vazia se não houver histórico
     */
    @Transactional(readOnly = true)
    public List<RevisaoDTO<Categoria>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }

    private void normalizarCampos(Categoria categoria) {
        categoria.setNome(ValidacoesBR.trimToNull(categoria.getNome()));
        categoria.setDescricao(ValidacoesBR.trimToNull(categoria.getDescricao()));
        categoria.setIcone(normalizarIcone(categoria.getIcone()));
    }

    private String normalizarIcone(String icone) {
        String valor = ValidacoesBR.trimToNull(icone);
        if (!CategoriaIcone.isChaveValida(valor)) {
            throw new IllegalArgumentException("Invalid category icon.");
        }
        return valor;
    }
}
