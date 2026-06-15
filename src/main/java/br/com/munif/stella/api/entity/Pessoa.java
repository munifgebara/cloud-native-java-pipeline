package br.com.munif.stella.api.entity;

import br.com.munif.comum.persistencia.Entidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Entidade que representa uma pessoa física ou jurídica no sistema.
 *
 * <p>Pessoas são utilizadas principalmente como tomadores em empréstimos de itens do inventário.
 * O documento {@link #cpfCnpj} é único no sistema e identifica se trata de pessoa física (CPF,
 * 11 dígitos) ou jurídica (CNPJ, 14 dígitos).</p>
 *
 * <p>O endereço completo é armazenado em campos separados para facilitar integrações
 * com serviços de busca por CEP e geolocalização.</p>
 *
 * <p>A entidade é auditada pelo Hibernate Envers: todas as alterações são registradas
 * na tabela {@code pessoa_aud}.</p>
 */
@Entity
@Audited
@Table(name = "pessoa")

@Getter
@Setter
@NoArgsConstructor
public class Pessoa extends Entidade {

    /**
     * Nome completo da pessoa física ou razão social da pessoa jurídica.
     * Obrigatório, com até 150 caracteres.
     */
    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    /**
     * CPF (11 dígitos) ou CNPJ (14 dígitos) da pessoa, sem formatação (apenas dígitos).
     * Deve ser único no sistema — utilizado como chave natural de identificação.
     */
    @Column(name = "cpf_cnpj", nullable = false, length = 14, unique = true)
    private String cpfCnpj;

    /**
     * Número do telefone principal de contato (celular ou fixo).
     * Formato livre, com até 20 caracteres (ex.: {@code "(11) 98765-4321"}).
     */
    @Column(name = "telefone_principal", length = 20)
    private String telefonePrincipal;

    /**
     * Número de telefone alternativo de contato.
     * Formato livre, com até 20 caracteres.
     */
    @Column(name = "telefone_secundario", length = 20)
    private String telefoneSecundario;

    /**
     * Endereço de e-mail da pessoa para comunicações e notificações.
     * Até 150 caracteres.
     */
    @Column(name = "email", length = 150)
    private String email;

    /**
     * Código de Endereçamento Postal (CEP) do endereço, somente dígitos (8 caracteres).
     * Exemplo: {@code "01310100"} para a Av. Paulista em São Paulo.
     */
    @Column(name = "cep", length = 8)
    private String cep;

    /**
     * Logradouro do endereço (rua, avenida, etc.) incluindo o número.
     * Até 200 caracteres.
     */
    @Column(name = "endereco", length = 200)
    private String endereco;

    /**
     * Complemento do endereço (ex.: apartamento, bloco, sala).
     * Até 100 caracteres.
     */
    @Column(name = "complemento", length = 100)
    private String complemento;

    /**
     * Bairro do endereço.
     * Até 100 caracteres.
     */
    @Column(name = "bairro", length = 100)
    private String bairro;

    /**
     * Cidade do endereço.
     * Até 100 caracteres.
     */
    @Column(name = "cidade", length = 100)
    private String cidade;

    /**
     * Sigla do estado (Unidade da Federação) do endereço, com 2 letras maiúsculas.
     * Exemplo: {@code "SP"}, {@code "RJ"}, {@code "MG"}.
     */
    @Column(name = "uf", length = 2)
    private String uf;
}
