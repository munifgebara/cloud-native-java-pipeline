package br.com.munif.stella.api.service;

import br.com.munif.comum.persistencia.MRevisionEntity;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.PessoaCreateDTO;
import br.com.munif.stella.api.dto.PessoaResponseDTO;
import br.com.munif.stella.api.dto.PessoaRevisaoDTO;
import br.com.munif.stella.api.dto.PessoaResumoDTO;
import br.com.munif.stella.api.dto.PessoaUpdateDTO;
import br.com.munif.stella.api.entity.Pessoa;
import br.com.munif.stella.api.exception.CadastroDuplicadoException;
import br.com.munif.stella.api.mapper.PessoaMapper;
import br.com.munif.stella.api.repository.PessoaRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class PessoaService extends SuperService<Pessoa, PessoaRepository> {

    public PessoaService(PessoaRepository repository, EntityManager entityManager) {
        super(repository, entityManager, Pessoa.class);
    }

    @Transactional
    public PessoaResponseDTO criar(PessoaCreateDTO dto) {
        validarCreate(dto);

        String cpfCnpjNormalizado = normalizarCpfCnpj(dto.cpfCnpj());

        if (repository.existsByCpfCnpj(cpfCnpjNormalizado)) {
            throw new CadastroDuplicadoException("Já existe pessoa cadastrada com este CPF/CNPJ.");
        }

        Pessoa pessoa = PessoaMapper.toEntity(dto);
        normalizarCampos(pessoa);
        pessoa.setCpfCnpj(cpfCnpjNormalizado);

        Pessoa salva = salvar(pessoa);
        return PessoaMapper.toResponseDTO(salva);
    }

    @Transactional(readOnly = true)
    public PessoaResponseDTO buscarResponsePorId(UUID id) {
        return PessoaMapper.toResponseDTO(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public List<PessoaResumoDTO> listarResumo() {
        return repository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(PessoaMapper::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PessoaResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(PessoaMapper::toResumoDTO)
                .toList();
    }

    @Transactional
    public PessoaResponseDTO atualizar(UUID id, PessoaUpdateDTO dto) {
        validarUpdate(dto);

        Pessoa pessoa = buscarPorId(id);
        PessoaMapper.updateEntity(pessoa, dto);
        normalizarCampos(pessoa);

        Pessoa salva = salvar(pessoa);
        return PessoaMapper.toResponseDTO(salva);
    }

    @Transactional
    public void excluirLogicamente(UUID id) {
        excluir(id);
    }

    @Transactional(readOnly = true)
    public List<PessoaResumoDTO> buscarPorNome(String nome) {
        String nomeTratado = ValidacoesBR.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return repository.findByAtivoTrueAndNomeContainingIgnoreCase(nomeTratado).stream()
                .map(PessoaMapper::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<PessoaRevisaoDTO> listarRevisoes(UUID id) {
        buscarPorId(id);

        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        List<PessoaRevisaoDTO> revisoes = auditReader.createQuery()
                .forRevisionsOfEntity(Pessoa.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList()
                .stream()
                .map(this::toPessoaRevisaoDTO)
                .toList();

        return adicionarCamposAlterados(revisoes);
    }

    @Transactional(readOnly = true)
    public long contarPessoasAtivas() {
        return repository.countByAtivoTrue();
    }

    private void validarCreate(PessoaCreateDTO dto) {
        String cpfCnpj = normalizarCpfCnpj(dto.cpfCnpj());

        if (cpfCnpj.length() == 11) {
            ValidacoesBR.exigirCPFValido(cpfCnpj, "CPF");
        } else if (cpfCnpj.length() == 14) {
            ValidacoesBR.exigirCNPJValido(cpfCnpj, "CNPJ");
        } else {
            throw new IllegalArgumentException("CPF/CNPJ deve conter 11 ou 14 dígitos.");
        }

        validarCamposComuns(
                dto.telefonePrincipal(),
                dto.telefoneSecundario(),
                dto.email(),
                dto.cep(),
                dto.uf()
        );
    }

    private void validarUpdate(PessoaUpdateDTO dto) {
        validarCamposComuns(
                dto.telefonePrincipal(),
                dto.telefoneSecundario(),
                dto.email(),
                dto.cep(),
                dto.uf()
        );
    }

    private void validarCamposComuns(
            String telefonePrincipal,
            String telefoneSecundario,
            String email,
            String cep,
            String uf
    ) {
        if (ValidacoesBR.isNotBlank(telefonePrincipal)) {
            ValidacoesBR.exigirTelefoneValido(telefonePrincipal, "Telefone principal");
        }

        if (ValidacoesBR.isNotBlank(telefoneSecundario)) {
            ValidacoesBR.exigirTelefoneValido(telefoneSecundario, "Telefone secundário");
        }

        if (ValidacoesBR.isNotBlank(email)) {
            ValidacoesBR.exigirEmailValido(email, "E-mail");
        }

        if (ValidacoesBR.isNotBlank(cep)) {
            ValidacoesBR.exigirCEPValido(cep, "CEP");
        }

        if (ValidacoesBR.isNotBlank(uf)) {
            String ufNormalizada = uf.trim().toUpperCase(Locale.ROOT);
            if (!ufNormalizada.matches("^[A-Z]{2}$")) {
                throw new IllegalArgumentException("UF inválida.");
            }
        }
    }

    private String normalizarCpfCnpj(String cpfCnpj) {
        String valor = ValidacoesBR.somenteDigitos(cpfCnpj);
        if (valor == null) {
            throw new IllegalArgumentException("CPF/CNPJ é obrigatório.");
        }
        return valor;
    }

    private void normalizarCampos(Pessoa pessoa) {
        pessoa.setNome(ValidacoesBR.trimToNull(pessoa.getNome()));
        pessoa.setTelefonePrincipal(normalizarTelefone(pessoa.getTelefonePrincipal()));
        pessoa.setTelefoneSecundario(normalizarTelefone(pessoa.getTelefoneSecundario()));
        pessoa.setEmail(normalizarEmail(pessoa.getEmail()));
        pessoa.setCep(normalizarCep(pessoa.getCep()));
        pessoa.setEndereco(ValidacoesBR.trimToNull(pessoa.getEndereco()));
        pessoa.setComplemento(ValidacoesBR.trimToNull(pessoa.getComplemento()));
        pessoa.setBairro(ValidacoesBR.trimToNull(pessoa.getBairro()));
        pessoa.setCidade(ValidacoesBR.trimToNull(pessoa.getCidade()));
        pessoa.setUf(normalizarUf(pessoa.getUf()));
    }

    private String normalizarTelefone(String telefone) {
        String valor = ValidacoesBR.trimToNull(telefone);
        return valor == null ? null : ValidacoesBR.somenteDigitos(valor);
    }

    private String normalizarEmail(String email) {
        String valor = ValidacoesBR.trimToNull(email);
        return valor == null ? null : valor.toLowerCase(Locale.ROOT);
    }

    private String normalizarCep(String cep) {
        String valor = ValidacoesBR.trimToNull(cep);
        return valor == null ? null : ValidacoesBR.somenteDigitos(valor);
    }

    private String normalizarUf(String uf) {
        String valor = ValidacoesBR.trimToNull(uf);
        return valor == null ? null : valor.toUpperCase(Locale.ROOT);
    }

    private PessoaRevisaoDTO toPessoaRevisaoDTO(Object item) {
        Object[] dadosRevisao = (Object[]) item;
        Pessoa pessoa = (Pessoa) dadosRevisao[0];
        MRevisionEntity revisao = (MRevisionEntity) dadosRevisao[1];
        RevisionType tipo = (RevisionType) dadosRevisao[2];

        return new PessoaRevisaoDTO(
                revisao.getId(),
                revisao.getTimestamp(),
                tipo.name(),
                PessoaMapper.toResponseDTO(pessoa),
                List.of()
        );
    }

    private List<PessoaRevisaoDTO> adicionarCamposAlterados(List<PessoaRevisaoDTO> revisoes) {
        List<PessoaRevisaoDTO> resultado = new ArrayList<>();

        for (int i = 0; i < revisoes.size(); i++) {
            PessoaRevisaoDTO revisao = revisoes.get(i);
            PessoaResponseDTO versaoAnterior = i + 1 < revisoes.size() ? revisoes.get(i + 1).pessoa() : null;

            resultado.add(new PessoaRevisaoDTO(
                    revisao.revisao(),
                    revisao.dataHora(),
                    revisao.tipo(),
                    revisao.pessoa(),
                    camposAlterados(revisao.pessoa(), versaoAnterior)
            ));
        }

        return resultado;
    }

    private List<String> camposAlterados(PessoaResponseDTO atual, PessoaResponseDTO anterior) {
        if (atual == null || anterior == null) {
            return List.of();
        }

        List<String> campos = new ArrayList<>();

        adicionarSeAlterado(campos, "nome", atual.nome(), anterior.nome());
        adicionarSeAlterado(campos, "cpfCnpj", atual.cpfCnpj(), anterior.cpfCnpj());
        adicionarSeAlterado(campos, "telefonePrincipal", atual.telefonePrincipal(), anterior.telefonePrincipal());
        adicionarSeAlterado(campos, "telefoneSecundario", atual.telefoneSecundario(), anterior.telefoneSecundario());
        adicionarSeAlterado(campos, "email", atual.email(), anterior.email());
        adicionarSeAlterado(campos, "cep", atual.cep(), anterior.cep());
        adicionarSeAlterado(campos, "endereco", atual.endereco(), anterior.endereco());
        adicionarSeAlterado(campos, "complemento", atual.complemento(), anterior.complemento());
        adicionarSeAlterado(campos, "bairro", atual.bairro(), anterior.bairro());
        adicionarSeAlterado(campos, "cidade", atual.cidade(), anterior.cidade());
        adicionarSeAlterado(campos, "uf", atual.uf(), anterior.uf());

        return campos;
    }

    private void adicionarSeAlterado(List<String> campos, String campo, Object atual, Object anterior) {
        if (!Objects.equals(atual, anterior)) {
            campos.add(campo);
        }
    }
}
