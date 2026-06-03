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
@Table(name = "instancia_item")
@Getter
@Setter
@NoArgsConstructor
public class InstanciaItem extends Entidade {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_mestre_id", nullable = false)
    private ItemMestre itemMestre;

    @Column(name = "identificador", length = 100)
    private String identificador;

    @Column(name = "patrimonio", length = 100)
    private String patrimonio;

    @Column(name = "numero_serie", length = 150)
    private String numeroSerie;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;
}
