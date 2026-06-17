package br.com.stella.api.dto;

import java.util.UUID;

/**
 * Summary DTO of a main item, used in listings and selectors.
 *
 * <p>Contains only the most relevant fields for display in grids and lists,
 * including the image URL for thumbnail viewing.</p>
 *
 * @param id             unique identifier of the main item
 * @param name           main item name
 * @param description      brief description of the item; may be {@code null}
 * @param categoryId    identifier of the associated category; may be {@code null}
 * @param categoryName  category name (denormalized); may be {@code null}
 * @param categoryIcon category icon key; may be {@code null}
 * @param imageUrl      relative URL for accessing the item image; {@code null} when no image
 * @param active          indicates whether the item is active in the system
 */
public record MainItemSummaryDTO(
        UUID id,
        String name,
        String description,
        UUID categoryId,
        String categoryName,
        String categoryIcon,
        String imageUrl,
        boolean active
) {}
