package com.fintrack.domain.creditcard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * Entity representing an expense category.
 * Used to categorize credit card expenses and transactions.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String color;

    @Column
    private String icon;

    @Column(name = "display_order")
    private Integer displayOrder;

    protected Category() {}

    private Category(final String theName, final String theColor) {
        Validate.notBlank(theName, "Category name must not be null or blank.");
        name = theName;
        color = theColor;
    }

    /**
     * Creates a new Category instance.
     *
     * @param name the category's name. Cannot be null or blank.
     * @param color the category's color in hex format. Can be null.
     * @return a validated Category entity. Never null.
     */
    public static Category of(final String name, final String color) {
        return new Category(name, color);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setName(final String name) {
        Validate.notBlank(name, "Category name must not be null or blank.");
        this.name = name;
    }

    public void setColor(final String color) {
        this.color = color;
    }

    public void setIcon(final String icon) {
        this.icon = icon;
    }

    public void setDisplayOrder(final Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Category category)) {
            return false;
        }
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Category{"
            + "id=" + id
            + ", name='" + name + '\''
            + ", color='" + color + '\''
            + ", icon='" + icon + '\''
            + ", displayOrder=" + displayOrder
            + '}';
    }
}
