package br.com.munif.stella.api.entity;

import br.com.munif.comum.persistencia.Entidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "item_mestre")
@Getter
@Setter
@NoArgsConstructor
public class ItemMestre extends Entidade {

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @Column(name = "imagem_bucket", length = 100)
    private String imagemBucket;

    @Column(name = "imagem_object_key", length = 500)
    private String imagemObjectKey;

    @Column(name = "imagem_content_type", length = 100)
    private String imagemContentType;

    @Column(name = "imagem_tamanho_bytes")
    private Long imagemTamanhoBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}
