package br.com.munif.stella.api.service;

import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.ItemMestre;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ItemMestreEmbeddingDocumentFactory {

    public String criarDocumento(ItemMestre item) {
        if (item == null) {
            return "";
        }

        Categoria categoria = item.getCategoria();
        return Stream.of(
                        linha("Nome", item.getNome()),
                        linha("Descrição", item.getDescricao()),
                        linha("Categoria", categoria == null ? null : categoria.getNome()),
                        linha("Observações", item.getObservacoes())
                )
                .filter(texto -> !texto.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String linha(String campo, String valor) {
        String tratado = ValidacoesBR.trimToNull(valor);
        return tratado == null ? "" : campo + ": " + tratado;
    }
}
