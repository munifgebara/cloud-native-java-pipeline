package br.com.stella.api.service;

import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.MainItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MainItemEmbeddingDocumentFactory {

    public String createDocument(MainItem item) {
        if (item == null) {
            return "";
        }

        Category category = item.getCategory();
        return Stream.of(
                        linha("Name", item.getName()),
                        linha("Description", item.getDescription()),
                        linha("Category", category == null ? null : category.getName()),
                        linha("Notes", item.getNotes())
                )
                .filter(texto -> !texto.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String linha(String campo, String valor) {
        String normalized = BrValidations.trimToNull(valor);
        return normalized == null ? "" : campo + ": " + normalized;
    }
}
