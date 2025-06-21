package com.fintrack.domain.creditcard;

import jakarta.persistence.*;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

/**
 * Entity representing a bank institution.
 * Contains bank information and identification.
 */
@Entity
@Table(name = "banks")
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    /**
     * Protected constructor for JPA only.
     */
    protected Bank() {}

    /**
     * Private constructor for Bank. Use the static factory method to create
     * instances.
     *
     * @param theCode the bank's code. Must not be null or blank.
     *
     * @param theName the bank's name. Must not be null or blank.
     */
    private Bank(final String theCode, final String theName) {
        Validate.notBlank(theCode, "Bank code must not be null or blank.");
        Validate.notBlank(theName, "Bank name must not be null or blank.");

        code = theCode;
        name = theName;
    }

    /**
     * Static factory method to create a new Bank instance.
     *
     * @param code the bank's code. Cannot be null or blank.
     *
     * @param name the bank's name. Cannot be null or blank.
     *
     * @return a validated Bank entity. Never null.
     */
    public static Bank of(final String code, final String name) {
        return new Bank(code, name);
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bank bank)) return false;
        return Objects.equals(id, bank.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Bank{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}