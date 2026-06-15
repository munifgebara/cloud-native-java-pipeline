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

/**
 * Entidade que representa um local de armazenamento de itens do inventário.
 *
 * <p>Locais de armazenamento formam uma estrutura hierárquica: um local pode ter um
 * local pai ({@link #pai}), permitindo representar prédios, andares, salas e armários
 * de forma aninhada. Por exemplo: "Prédio A" &gt; "Sala 101" &gt; "Armário 3".</p>
 *
 * <p>Cada local pode ter uma imagem associada, armazenada em um serviço de object storage,
 * para facilitar a identificação visual pelos usuários.</p>
 *
 * <p>A entidade é auditada pelo Hibernate Envers: todas as alterações são registradas
 * na tabela {@code local_armazenamento_aud}.</p>
 */
@Entity
@Audited
@Table(name = "local_armazenamento")
@Getter
@Setter
@NoArgsConstructor
public class LocalArmazenamento extends Entidade {

    /**
     * Nome do local de armazenamento. Obrigatório, com até 150 caracteres.
     * Exemplos: {@code "Depósito Principal"}, {@code "Sala de TI"}, {@code "Armário 02"}.
     */
    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    /**
     * Texto descritivo opcional com informações adicionais sobre o local
     * (ex.: capacidade, tipo de itens armazenados, restrições de acesso).
     * Até 500 caracteres.
     */
    @Column(name = "descricao", length = 500)
    private String descricao;

    /**
     * Nome do bucket no serviço de object storage onde a imagem do local está armazenada.
     * {@code null} quando o local não possui imagem cadastrada.
     * Até 100 caracteres.
     */
    @Column(name = "imagem_bucket", length = 100)
    private String imagemBucket;

    /**
     * Chave (caminho/nome do objeto) da imagem dentro do bucket de object storage.
     * Usado em conjunto com {@link #imagemBucket} para localizar o arquivo.
     * Até 500 caracteres.
     */
    @Column(name = "imagem_object_key", length = 500)
    private String imagemObjectKey;

    /**
     * Tipo MIME da imagem armazenada (ex.: {@code "image/jpeg"}, {@code "image/png"}).
     * Necessário para servir o arquivo com o cabeçalho {@code Content-Type} correto.
     * Até 100 caracteres.
     */
    @Column(name = "imagem_content_type", length = 100)
    private String imagemContentType;

    /**
     * Tamanho da imagem em bytes. Útil para exibição de informações ao usuário
     * e para verificações de quota de armazenamento.
     */
    @Column(name = "imagem_tamanho_bytes")
    private Long imagemTamanhoBytes;

    /**
     * Local pai na hierarquia de armazenamento.
     * {@code null} indica que este é um local raiz (nível mais alto da hierarquia).
     * Carregado de forma lazy para evitar joins encadeados desnecessários.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_pai_id")
    private LocalArmazenamento pai;
}
