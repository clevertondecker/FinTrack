package com.fintrack.domain.creditcard;

import jakarta.persistence.*;
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

    /**
     * Protected constructor for JPA only.
     */
    protected Category() {}

    /**
     * Private constructor for Category. Use the static factory method to create instances.
     *
     * @param theName the category's name. Must not be null or blank.
     * @param theColor the category's color in hex format. Can be null.
     */
    private Category(final String theName, final String theColor) {
        Validate.notBlank(theName, "Category name must not be null or blank.");

        name = theName;
        color = theColor;
    }

    /**
     * Static factory method to create a new Category instance.
     *
     * @param name the category's name. Cannot be null or blank.
     * @param color the category's color in hex format. Can be null.
     * @return a validated Category entity. Never null.
     */
    public static Category of(final String name, final String color) {
        return new Category(name, color);
    }

    /**
     * Gets the category's unique identifier.
     *
     * @return the category's ID. May be null if not persisted.
     */
    public Long getId() { return id; }

    /**
     * Gets the category's name.
     *
     * @return the category's name. Never null or blank.
     */
    public String getName() { return name; }

    /**
     * Gets the category's color.
     *
     * @return the category's color in hex format. May be null.
     */
    public String getColor() { return color; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category category)) return false;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}