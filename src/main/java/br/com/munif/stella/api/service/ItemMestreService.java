package br.com.munif.stella.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.ItemMestreCreateDTO;
import br.com.munif.stella.api.dto.ImagemItemMestreDTO;
import br.com.munif.stella.api.dto.ItemMestreResponseDTO;
import br.com.munif.stella.api.dto.ItemMestreResumoDTO;
import br.com.munif.stella.api.dto.ItemMestreUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.mapper.ItemMestreMapper;
import br.com.munif.stella.api.repository.CategoriaRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class ItemMestreService extends SuperService<ItemMestre, ItemMestreRepository> {

    private final CategoriaRepository categoriaRepository;
    private final ImagemItemMestreStorageService imagemStorageService;

    public ItemMestreService(
            ItemMestreRepository repository,
            EntityManager entityManager,
            CategoriaRepository categoriaRepository,
            ImagemItemMestreStorageService imagemStorageService
    ) {
        super(repository, entityManager, ItemMestre.class);
        this.categoriaRepository = categoriaRepository;
        this.imagemStorageService = imagemStorageService;
    }

    @Transactional
    public ItemMestreResponseDTO criar(ItemMestreCreateDTO dto) {
        ItemMestre item = ItemMestreMapper.toEntity(dto);
        normalizarCampos(item);
        item.setCategoria(buscarCategoriaAtiva(dto.categoriaId()));

        ItemMestre salvo = salvar(item);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salvo.setAtivo(false);
            salvo = salvar(salvo);
        }
        return ItemMestreMapper.toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public ItemMestreResponseDTO buscarResponsePorId(UUID id) {
        return ItemMestreMapper.toResponseDTO(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public List<ItemMestreResumoDTO> listarResumo() {
        return repository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemMestreResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemMestreResumoDTO> buscarPorNome(String nome) {
        String nomeTratado = ValidacoesBR.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(nomeTratado).stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemMestreResumoDTO> filtrar(String nome, UUID categoriaId) {
        return repository.filtrarAtivos(ValidacoesBR.trimToNull(nome), categoriaId).stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    @Transactional
    public ItemMestreResponseDTO atualizar(UUID id, ItemMestreUpdateDTO dto) {
        ItemMestre item = buscarPorId(id);
        Categoria categoria = buscarCategoriaAtiva(dto.categoriaId());

        ItemMestreMapper.updateEntity(item, dto);
        normalizarCampos(item);
        item.setCategoria(categoria);

        ItemMestre salvo = salvar(item);
        return ItemMestreMapper.toResponseDTO(salvo);
    }

    @Transactional
    public ItemMestreResponseDTO atualizarImagemPrincipal(UUID id, MultipartFile arquivo) {
        return atualizarImagemPrincipal(id, arquivo, false, null);
    }

    @Transactional
    public ItemMestreResponseDTO atualizarImagemPrincipal(UUID id, MultipartFile arquivo, boolean generatedByAi, String provider) {
        ItemMestre item = buscarPorId(id);
        String bucketAnterior = item.getImagemBucket();
        String objectKeyAnterior = item.getImagemObjectKey();

        ImagemItemMestreDTO imagem = imagemStorageService.armazenar(id, arquivo);
        item.setImagemBucket(imagem.bucket());
        item.setImagemObjectKey(imagem.objectKey());
        item.setImagemContentType(imagem.contentType());
        item.setImagemTamanhoBytes(imagem.tamanhoBytes());
        item.setImagemGeneratedByAi(generatedByAi);
        item.setImagemProvider(generatedByAi ? ValidacoesBR.trimToNull(provider) : null);

        ItemMestre salvo = salvar(item);
        imagemStorageService.removerSilenciosamente(bucketAnterior, objectKeyAnterior);
        return ItemMestreMapper.toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public ImagemItemMestreDTO buscarMetadadosImagemPrincipal(UUID id) {
        ItemMestre item = buscarPorId(id);
        if (item.getImagemObjectKey() == null) {
            throw new IllegalArgumentException("Item mestre não possui imagem principal.");
        }
        return new ImagemItemMestreDTO(
                item.getImagemBucket(),
                item.getImagemObjectKey(),
                item.getImagemContentType(),
                item.getImagemTamanhoBytes()
        );
    }

    @Transactional(readOnly = true)
    public InputStream abrirImagemPrincipal(UUID id) {
        ImagemItemMestreDTO imagem = buscarMetadadosImagemPrincipal(id);
        return imagemStorageService.abrir(imagem.bucket(), imagem.objectKey());
    }

    @Transactional
    public void excluirLogicamente(UUID id) {
        excluir(id);
    }

    @Transactional(readOnly = true)
    public List<RevisaoDTO<ItemMestre>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }

    private Categoria buscarCategoriaAtiva(UUID categoriaId) {
        if (categoriaId == null) {
            return null;
        }

        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
        if (!categoria.isAtivo()) {
            throw new IllegalArgumentException("Categoria deve estar ativa.");
        }
        return categoria;
    }

    private void normalizarCampos(ItemMestre item) {
        item.setNome(ValidacoesBR.trimToNull(item.getNome()));
        item.setDescricao(ValidacoesBR.trimToNull(item.getDescricao()));
        item.setObservacoes(ValidacoesBR.trimToNull(item.getObservacoes()));
    }
}
