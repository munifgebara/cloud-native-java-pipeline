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

    @Transactional(readOnly = true)
    public LocalArmazenamentoResponseDTO buscarResponsePorId(UUID id) {
        return LocalArmazenamentoMapper.toResponseDTO(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public List<LocalArmazenamentoResumoDTO> listarResumo() {
        return montarHierarquia(repository.findByAtivoTrueOrderByNomeAsc());
    }

    @Transactional(readOnly = true)
    public List<LocalArmazenamentoResumoDTO> listarResumoIncluindoInativos() {
        return montarHierarquia(listarTodosIncluindoInativos());
    }

    @Transactional(readOnly = true)
    public List<LocalArmazenamentoResumoDTO> buscarPorNome(String nome) {
        String nomeTratado = ValidacoesBR.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return montarHierarquia(repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(nomeTratado));
    }

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

    @Transactional(readOnly = true)
    public ImagemItemMestreDTO buscarMetadadosImagem(UUID id) {
        LocalArmazenamento local = buscarPorId(id);
        if (local.getImagemObjectKey() == null) {
            throw new IllegalArgumentException("Local não possui imagem.");
        }
        return new ImagemItemMestreDTO(
                local.getImagemBucket(),
                local.getImagemObjectKey(),
                local.getImagemContentType(),
                local.getImagemTamanhoBytes()
        );
    }

    @Transactional(readOnly = true)
    public InputStream abrirImagem(UUID id) {
        ImagemItemMestreDTO imagem = buscarMetadadosImagem(id);
        return imagemStorageService.abrir(imagem.bucket(), imagem.objectKey());
    }

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
            throw new IllegalArgumentException("Local pai deve estar ativo.");
        }
        return pai;
    }

    private void validarHierarquia(LocalArmazenamento local, LocalArmazenamento pai) {
        if (pai == null) {
            return;
        }

        if (local.getId().equals(pai.getId())) {
            throw new IllegalArgumentException("Local não pode ser pai dele mesmo.");
        }

        LocalArmazenamento atual = pai.getPai();
        while (atual != null) {
            if (local.getId().equals(atual.getId())) {
                throw new IllegalArgumentException("Local pai não pode ser descendente do próprio local.");
            }
            atual = atual.getPai();
        }
    }

    private void normalizarCampos(LocalArmazenamento local) {
        local.setNome(ValidacoesBR.trimToNull(local.getNome()));
        local.setDescricao(ValidacoesBR.trimToNull(local.getDescricao()));
    }
}
