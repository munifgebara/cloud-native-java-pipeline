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
 * Entidade que representa um item mestre do inventário.
 *
 * <p>O item mestre é o "modelo" ou "tipo" de um bem patrimonial — por exemplo,
 * "Notebook Dell Inspiron 15". Cada item mestre pode ter várias
 * {@link InstanciaItem instâncias físicas} associadas, cada uma com seu próprio
 * número de série, patrimônio e localização.</p>
 *
 * <p>O item pode ter uma imagem armazenada em um serviço de object storage (ex.: S3).
 * Os campos {@code imagemBucket}, {@code imagemObjectKey} e demais metadados de imagem
 * permitem recuperar e exibir essa imagem.</p>
 *
 * <p>A entidade é auditada pelo Hibernate Envers: todas as alterações são registradas
 * na tabela {@code item_mestre_aud}.</p>
 */
@Entity
@Audited
@Table(name = "item_mestre")
@Getter
@Setter
@NoArgsConstructor
public class ItemMestre extends Entidade {

    /**
     * Nome do item mestre. Obrigatório, com até 150 caracteres.
     * Exemplo: {@code "Notebook Dell Inspiron 15"}.
     */
    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    /**
     * Descrição detalhada do item, contendo características relevantes.
     * Opcional, com até 500 caracteres.
     */
    @Column(name = "descricao", length = 500)
    private String descricao;

    /**
     * Observações internas sobre o item (histórico de manutenção, restrições de uso, etc.).
     * Opcional, com até 1000 caracteres.
     */
    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    /**
     * Indica a origem do cadastro do item (ex.: {@code "MANUAL"}, {@code "IA"}, {@code "FOTO"}).
     * Usado para rastrear como o item foi inserido no sistema.
     * Até 50 caracteres.
     */
    @Column(name = "origem_cadastro", length = 50)
    private String origemCadastro;

    /**
     * Nome do bucket no serviço de object storage onde a imagem do item está armazenada.
     * {@code null} quando o item não possui imagem cadastrada.
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
     * Indica se a imagem foi gerada por inteligência artificial.
     * {@code true} quando a imagem foi produzida por um modelo de IA;
     * {@code false} quando foi enviada manualmente pelo usuário.
     */
    @Column(name = "imagem_generated_by_ai", nullable = false)
    private boolean imagemGeneratedByAi;

    /**
     * Identificador do provedor de IA que gerou a imagem (ex.: {@code "openai"}, {@code "anthropic"}).
     * {@code null} quando {@link #imagemGeneratedByAi} for {@code false}.
     * Até 50 caracteres.
     */
    @Column(name = "imagem_provider", length = 50)
    private String imagemProvider;

    /**
     * Categoria à qual este item pertence.
     * Relacionamento opcional: um item pode existir sem categoria definida.
     * Carregado de forma lazy para evitar joins desnecessários.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}
