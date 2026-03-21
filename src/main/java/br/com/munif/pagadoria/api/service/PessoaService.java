package br.com.munif.pagadoria.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.pagadoria.api.dto.PessoaCreateDTO;
import br.com.munif.pagadoria.api.dto.PessoaResponseDTO;
import br.com.munif.pagadoria.api.dto.PessoaResumoDTO;
import br.com.munif.pagadoria.api.dto.PessoaUpdateDTO;
import br.com.munif.pagadoria.api.entity.Pessoa;
import br.com.munif.pagadoria.api.exception.CadastroDuplicadoException;
import br.com.munif.pagadoria.api.mapper.PessoaMapper;
import br.com.munif.pagadoria.api.repository.PessoaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
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
    public List<RevisaoDTO<Pessoa>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
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
}
