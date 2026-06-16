package br.com.munif.stella.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.LocalArmazenamentoCreateDTO;
import br.com.munif.stella.api.dto.ImagemItemMestreDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResponseDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResumoDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoUpdateDTO;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.mapper.LocalArmazenamentoMapper;
import br.com.munif.stella.api.observability.StructuredBusinessLogger;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelas operações de negócio sobre {@link LocalArmazenamento}.
 *
 * <p>Gerencia o ciclo de vida dos locais de armazenamento do inventário, que podem
 * ser organizados em hierarquia pai-filho (ex.: Prédio > Sala > Armário).
 * Inclui suporte a upload de imagem via MinIO e consulta de revisões de auditoria.</p>
 *
 * <p>A listagem retorna os locais já ordenados em profundidade primeiro (DFS),
 * com o caminho completo de cada nó para facilitar a exibição em listas hierárquicas.</p>
 */
@Service
public class LocalArmazenamentoService extends SuperService<LocalArmazenamento, LocalArmazenamentoRepository> {

    private static final Logger log = LoggerFactory.getLogger(LocalArmazenamentoService.class);

    private final ImagemItemMestreStorageService imagemStorageService;

    public LocalArmazenamentoService(
            LocalArmazenamentoRepository repository,
            EntityManager entityManager,
            ImagemItemMestreStorageService imagemStorageService
    ) {
        super(repository, entityManager, LocalArmazenamento.class);
        this.imagemStorageService = imagemStorageService;
    }

    /**
     * Cria um novo local de armazenamento.
     *
     * @param dto dados de criação validados pelo Bean Validation
     * @return DTO completo do local criado
     * @throws IllegalArgumentException se o local pai informado não existir ou estiver inativo
     */
    @Transactional
    public LocalArmazenamentoResponseDTO criar(LocalArmazenamentoCreateDTO dto) {
        LocalArmazenamento local = LocalArmazenamentoMapper.toEntity(dto);
        normalizarCampos(local);
        local.setPai(buscarPaiAtivo(dto.paiId()));

        LocalArmazenamento salvo = salvar(local);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salvo.setAtivo(false);
            salvo = salvar(salvo);
        }
        StructuredBusinessLogger.info(log, "inventory", "location-created", StructuredBusinessLogger.fields(
                "location_id", salvo.getId(),
                "location_name", salvo.getNome(),
                "parent_location_id", salvo.getPai() == null ? null : salvo.getPai().getId(),
                "success", true
        ));
        return LocalArmazenamentoMapper.toResponseDTO(salvo);
    }

    /**
     * Retorna o DTO completo de um local pelo seu identificador.
     *
     * @param id UUID do local
     * @return DTO completo do local
     * @throws jakarta.persistence.EntityNotFoundException se o local não existir
     */
    @Transactional(readOnly = true)
    public LocalArmazenamentoResponseDTO buscarResponsePorId(UUID id) {
        return LocalArmazenamentoMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Lista todos os locais ativos em ordem hierárquica (DFS), com caminho e nível de cada nó.
     *
     * @return lista de DTOs de resumo dos locais ativos
     */
    @Transactional(readOnly = true)
    public List<LocalArmazenamentoResumoDTO> listarResumo() {
        return montarHierarquia(repository.findByAtivoTrueOrderByNomeAsc());
    }

    /**
     * Lista todos os locais, incluindo os inativados, em ordem hierárquica (DFS).
     *
     * @return lista de DTOs de resumo de todos os locais
     */
    @Transactional(readOnly = true)
    public List<LocalArmazenamentoResumoDTO> listarResumoIncluindoInativos() {
        return montarHierarquia(listarTodosIncluindoInativos());
    }

    /**
     * Busca locais ativos cujo nome contenha o texto informado (case-insensitive),
     * retornando a lista em ordem hierárquica.
     *
     * @param nome substring a buscar no nome do local; retorna lista vazia se em branco
     * @return lista de DTOs de resumo dos locais encontrados
     */
    @Transactional(readOnly = true)
    public List<LocalArmazenamentoResumoDTO> buscarPorNome(String nome) {
        String nomeTratado = ValidacoesBR.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return montarHierarquia(repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(nomeTratado));
    }

    /**
     * Atualiza os dados de um local existente, validando a hierarquia pai-filho.
     *
     * @param id  UUID do local a atualizar
     * @param dto dados de atualização validados pelo Bean Validation
     * @return DTO completo do local atualizado
     * @throws IllegalArgumentException se o local pai estiver inativo, for o próprio local
     *                                  ou for descendente do local a ser atualizado
     */
    @Transactional
    public LocalArmazenamentoResponseDTO atualizar(UUID id, LocalArmazenamentoUpdateDTO dto) {
        LocalArmazenamento local = buscarPorId(id);
        LocalArmazenamento pai = buscarPaiAtivo(dto.paiId());
        validarHierarquia(local, pai);

        LocalArmazenamentoMapper.updateEntity(local, dto);
        normalizarCampos(local);
        local.setPai(pai);

        LocalArmazenamento salvo = salvar(local);
        StructuredBusinessLogger.info(log, "inventory", "location-updated", StructuredBusinessLogger.fields(
                "location_id", salvo.getId(),
                "location_name", salvo.getNome(),
                "parent_location_id", salvo.getPai() == null ? null : salvo.getPai().getId(),
                "success", true
        ));
        return LocalArmazenamentoMapper.toResponseDTO(salvo);
    }

    /**
     * Atualiza a imagem do local, armazenando-a no MinIO e removendo a anterior.
     *
     * @param id      UUID do local
     * @param arquivo arquivo de imagem enviado pelo cliente
     * @return DTO completo do local com os novos metadados de imagem
     */
    @Transactional
    public LocalArmazenamentoResponseDTO atualizarImagem(UUID id, MultipartFile arquivo) {
        LocalArmazenamento local = buscarPorId(id);
        String bucketAnterior = local.getImagemBucket();
        String objectKeyAnterior = local.getImagemObjectKey();

        ImagemItemMestreDTO imagem = imagemStorageService.armazenarLocal(id, arquivo);
        local.setImagemBucket(imagem.bucket());
        local.setImagemObjectKey(imagem.objectKey());
        local.setImagemContentType(imagem.contentType());
        local.setImagemTamanhoBytes(imagem.tamanhoBytes());

        LocalArmazenamento salvo = salvar(local);
        imagemStorageService.removerSilenciosamente(bucketAnterior, objectKeyAnterior);
        StructuredBusinessLogger.info(log, "inventory", "location-image-updated", StructuredBusinessLogger.fields(
                "location_id", salvo.getId(),
                "location_name", salvo.getNome(),
                "image_content_type", imagem.contentType(),
                "image_size_bytes", imagem.tamanhoBytes(),
                "success", true
        ));
        return LocalArmazenamentoMapper.toResponseDTO(salvo);
    }

    /**
     * Remove a imagem do local, excluindo-a do MinIO e limpando os metadados.
     *
     * @param id UUID do local
     * @return DTO completo do local sem metadados de imagem
     */
    @Transactional
    public LocalArmazenamentoResponseDTO removerImagem(UUID id) {
        LocalArmazenamento local = buscarPorId(id);
        String bucketAnterior = local.getImagemBucket();
        String objectKeyAnterior = local.getImagemObjectKey();

        local.setImagemBucket(null);
        local.setImagemObjectKey(null);
        local.setImagemContentType(null);
        local.setImagemTamanhoBytes(null);

        LocalArmazenamento salvo = salvar(local);
        imagemStorageService.removerSilenciosamente(bucketAnterior, objectKeyAnterior);
        StructuredBusinessLogger.info(log, "inventory", "location-image-removed", StructuredBusinessLogger.fields(
                "location_id", salvo.getId(),
                "location_name", salvo.getNome(),
                "success", true
        ));
        return LocalArmazenamentoMapper.toResponseDTO(salvo);
    }

    /**
     * Retorna os metadados da imagem de um local (bucket, objectKey, contentType e tamanho).
     *
     * @param id UUID do local
     * @return DTO com os metadados necessários para recuperar o arquivo no MinIO
     * @throws IllegalArgumentException se o local não possuir imagem cadastrada
     */
    @Transactional(readOnly = true)
    public ImagemItemMestreDTO buscarMetadadosImagem(UUID id) {
        LocalArmazenamento local = buscarPorId(id);
        if (local.getImagemObjectKey() == null) {
            throw new IllegalArgumentException("Location does not have an image.");
        }
        return new ImagemItemMestreDTO(
                local.getImagemBucket(),
                local.getImagemObjectKey(),
                local.getImagemContentType(),
                local.getImagemTamanhoBytes()
        );
    }

    /**
     * Abre um stream de leitura da imagem do local no MinIO.
     * O chamador é responsável por fechar o stream após o uso.
     *
     * @param id UUID do local
     * @return stream de leitura da imagem
     * @throws IllegalArgumentException se o local não possuir imagem cadastrada
     */
    @Transactional(readOnly = true)
    public InputStream abrirImagem(UUID id) {
        ImagemItemMestreDTO imagem = buscarMetadadosImagem(id);
        return imagemStorageService.abrir(imagem.bucket(), imagem.objectKey());
    }

    /**
     * Inativa logicamente um local de armazenamento.
     *
     * @param id UUID do local a inativar
     * @throws jakarta.persistence.EntityNotFoundException se o local não existir
     */
    @Transactional
    public void excluirLogicamente(UUID id) {
        LocalArmazenamento local = buscarPorId(id);
        excluir(id);
        StructuredBusinessLogger.info(log, "inventory", "location-deactivated", StructuredBusinessLogger.fields(
                "location_id", id,
                "location_name", local.getNome(),
                "success", true
        ));
    }

    /**
     * Retorna o histórico de revisões anteriores de um local (Hibernate Envers).
     *
     * @param id UUID do local
     * @return lista de revisões em ordem cronológica; lista vazia se não houver histórico
     */
    @Transactional(readOnly = true)
    public List<RevisaoDTO<LocalArmazenamento>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }

    private List<LocalArmazenamentoResumoDTO> montarHierarquia(List<LocalArmazenamento> locais) {
        Map<UUID, List<LocalArmazenamento>> filhosPorPai = locais.stream()
                .filter(local -> local.getPai() != null)
                .collect(Collectors.groupingBy(local -> local.getPai().getId()));

        List<LocalArmazenamento> raizes = locais.stream()
                .filter(local -> local.getPai() == null || locais.stream().noneMatch(item -> item.getId().equals(local.getPai().getId())))
                .sorted(Comparator.comparing(LocalArmazenamento::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<LocalArmazenamentoResumoDTO> resultado = new ArrayList<>();
        Set<UUID> visitados = new HashSet<>();
        for (LocalArmazenamento raiz : raizes) {
            adicionarNaHierarquia(raiz, raiz.getNome(), 0, filhosPorPai, resultado, visitados);
        }
        return resultado;
    }

    private void adicionarNaHierarquia(
            LocalArmazenamento local,
            String caminho,
            int nivel,
            Map<UUID, List<LocalArmazenamento>> filhosPorPai,
            List<LocalArmazenamentoResumoDTO> resultado,
            Set<UUID> visitados
    ) {
        if (!visitados.add(local.getId())) {
            return;
        }

        resultado.add(LocalArmazenamentoMapper.toResumoDTO(local, caminho, nivel));
        filhosPorPai.getOrDefault(local.getId(), List.of()).stream()
                .sorted(Comparator.comparing(LocalArmazenamento::getNome, String.CASE_INSENSITIVE_ORDER))
                .forEach(filho -> adicionarNaHierarquia(filho, caminho + " > " + filho.getNome(), nivel + 1, filhosPorPai, resultado, visitados));
    }

    private LocalArmazenamento buscarPaiAtivo(UUID paiId) {
        if (paiId == null) {
            return null;
        }

        LocalArmazenamento pai = buscarPorId(paiId);
        if (!pai.isAtivo()) {
            throw new IllegalArgumentException("Parent location must be active.");
        }
        return pai;
    }

    private void validarHierarquia(LocalArmazenamento local, LocalArmazenamento pai) {
        if (pai == null) {
            return;
        }

        if (local.getId().equals(pai.getId())) {
            throw new IllegalArgumentException("A location cannot be its own parent.");
        }

        LocalArmazenamento atual = pai.getPai();
        while (atual != null) {
            if (local.getId().equals(atual.getId())) {
                throw new IllegalArgumentException("Parent location cannot be a descendant of itself.");
            }
            atual = atual.getPai();
        }
    }

    private void normalizarCampos(LocalArmazenamento local) {
        local.setNome(ValidacoesBR.trimToNull(local.getNome()));
        local.setDescricao(ValidacoesBR.trimToNull(local.getDescricao()));
    }
}
