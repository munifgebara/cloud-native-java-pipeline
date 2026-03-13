package br.com.munif.pagadoria.api.entity;

import br.com.munif.comum.persistencia.Entidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "pessoa")
public class Pessoa extends Entidade {

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "cpf_cnpj", nullable = false, length = 14, unique = true)
    private String cpfCnpj;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }
}