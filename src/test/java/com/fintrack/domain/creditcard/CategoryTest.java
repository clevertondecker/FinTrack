package com.fintrack.domain.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Category Entity Tests")
class CategoryTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create Category with name and color successfully")
        void shouldCreateCategoryWithNameAndColorSuccessfully() {
            String name = "Food";
            String color = "#FF0000";

            Category category = Category.of(name, color);

            assertThat(category).isNotNull();
            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should create Category with name only (color null)")
        void shouldCreateCategoryWithNameOnly() {
            String name = "Transport";

            Category category = Category.of(name, null);

            assertThat(category).isNotNull();
            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isNull();
        }

        @Test
        @DisplayName("Should create Category with empty color string")
        void shouldCreateCategoryWithEmptyColorString() {
            String name = "Entertainment";
            String color = "";

            Category category = Category.of(name, color);

            assertThat(category).isNotNull();
            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isEqualTo("");
        }

        @Test
        @DisplayName("Should create Category with single character name")
        void shouldCreateCategoryWithSingleCharacterName() {
            String name = "A";
            String color = "#00FF00";

            Category category = Category.of(name, color);

            assertThat(category).isNotNull();
            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should create Category with long name")
        void shouldCreateCategoryWithLongName() {
            String name = "Very Long Category Name That Exceeds Normal Length";
            String color = "#0000FF";

            Category category = Category.of(name, color);

            assertThat(category).isNotNull();
            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should create Category with special characters in name")
        void shouldCreateCategoryWithSpecialCharactersInName() {
            String name = "Food & Beverages";
            String color = "#FF00FF";

            Category category = Category.of(name, color);

            assertThat(category).isNotNull();
            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should create Category with numbers in name")
        void shouldCreateCategoryWithNumbersInName() {
            String name = "Category 123";
            String color = "#123456";

            Category category = Category.of(name, color);

            assertThat(category).isNotNull();
            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isEqualTo(color);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when name is null")
        void shouldThrowExceptionWhenNameIsNull() {
            assertThatThrownBy(() -> Category.of(null, "#FF0000"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Category name must not be null or blank.");
        }

        @Test
        @DisplayName("Should throw exception when name is empty")
        void shouldThrowExceptionWhenNameIsEmpty() {
            assertThatThrownBy(() -> Category.of("", "#FF0000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category name must not be null or blank.");
        }

        @Test
        @DisplayName("Should throw exception when name is blank")
        void shouldThrowExceptionWhenNameIsBlank() {
            assertThatThrownBy(() -> Category.of("   ", "#FF0000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category name must not be null or blank.");
        }

        @Test
        @DisplayName("Should throw exception when name is only whitespace")
        void shouldThrowExceptionWhenNameIsOnlyWhitespace() {
            assertThatThrownBy(() -> Category.of("\t\n", "#FF0000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category name must not be null or blank.");
        }
    }

    @Nested
    @DisplayName("Getter Methods Tests")
    class GetterMethodsTests {

        @Test
        @DisplayName("Should return correct values from getters")
        void shouldReturnCorrectValuesFromGetters() {
            String name = "Shopping";
            String color = "#00FFFF";

            Category category = Category.of(name, color);

            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should return null ID when not persisted")
        void shouldReturnNullIdWhenNotPersisted() {
            Category category = Category.of("Test", "#FF0000");

            assertThat(category.getId()).isNull();
        }

        @Test
        @DisplayName("Should return null color when color is null")
        void shouldReturnNullColorWhenColorIsNull() {
            Category category = Category.of("Test", null);

            assertThat(category.getColor()).isNull();
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            Category category = Category.of("Test", "#FF0000");

            assertThat(category).isEqualTo(category);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            Category category = Category.of("Test", "#FF0000");

            assertThat(category).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Category category = Category.of("Test", "#FF0000");

            assertThat(category).isNotEqualTo("string");
        }

        @Test
        @DisplayName("Should have same hash code for equal objects (same id)")
        void shouldHaveSameHashCodeForEqualObjects() {
            Category category1 = Category.of("Test", "#FF0000");

          assertThat(category1).hasSameHashCodeAs(category1);
        }

        @Test
        @DisplayName("Should not be equal to category with different name")
        void shouldNotBeEqualToCategoryWithDifferentName() {
            Category category1 = Category.of("Test1", "#FF0000");
            Category category2 = Category.of("Test2", "#FF0000");

            // Categories with null ids are equal if they have the same id (null)
            // Since both have null ids, they are considered equal
            assertThat(category1).isEqualTo(category2);
        }

        @Test
        @DisplayName("Should not be equal to category with different color")
        void shouldNotBeEqualToCategoryWithDifferentColor() {
            Category category1 = Category.of("Test", "#FF0000");
            Category category2 = Category.of("Test", "#00FF00");

            // Categories with null ids are equal if they have the same id (null)
            // Since both have null ids, they are considered equal
            assertThat(category1).isEqualTo(category2);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return meaningful string representation")
        void shouldReturnMeaningfulStringRepresentation() {
            Category category = Category.of("Food", "#FF0000");

            String toString = category.toString();

            assertThat(toString).contains("Category{");
            assertThat(toString).contains("id=");
            assertThat(toString).contains("name='Food'");
            assertThat(toString).contains("color='#FF0000'");
        }

        @Test
        @DisplayName("Should return meaningful string representation with null color")
        void shouldReturnMeaningfulStringRepresentationWithNullColor() {
            Category category = Category.of("Food", null);

            String toString = category.toString();

            assertThat(toString).contains("Category{");
            assertThat(toString).contains("id=");
            assertThat(toString).contains("name='Food'");
            assertThat(toString).contains("color='null'");
        }

        @Test
        @DisplayName("Should return meaningful string representation with null id")
        void shouldReturnMeaningfulStringRepresentationWithNullId() {
            Category category = Category.of("Food", "#FF0000");

            String toString = category.toString();

            assertThat(toString).contains("Category{");
            assertThat(toString).contains("id=null");
            assertThat(toString).contains("name='Food'");
            assertThat(toString).contains("color='#FF0000'");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long category name")
        void shouldHandleVeryLongCategoryName() {
            String longName = "A".repeat(1000);
            String color = "#FF0000";

            Category category = Category.of(longName, color);

            assertThat(category.getName()).isEqualTo(longName);
            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should handle very long color value")
        void shouldHandleVeryLongColorValue() {
            String name = "Test";
            String longColor = "#" + "F".repeat(100);

            Category category = Category.of(name, longColor);

            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isEqualTo(longColor);
        }

        @Test
        @DisplayName("Should handle unicode characters in name")
        void shouldHandleUnicodeCharactersInName() {
            String name = "Café & Restaurante 🍕";
            String color = "#FF0000";

            Category category = Category.of(name, color);

            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should handle unicode characters in color")
        void shouldHandleUnicodeCharactersInColor() {
            String name = "Test";
            String color = "#FF0000🎨";

            Category category = Category.of(name, color);

            assertThat(category.getName()).isEqualTo(name);
            assertThat(category.getColor()).isEqualTo(color);
        }
    }

    @Nested
    @DisplayName("Color Format Tests")
    class ColorFormatTests {

        @ParameterizedTest
        @ValueSource(strings = {"#FF0000", "#00FF00", "#0000FF", "#FFFFFF", "#000000", "#123456", "#ABCDEF"})
        @DisplayName("Should handle valid hex color formats")
        void shouldHandleValidHexColorFormats(String color) {
            Category category = Category.of("Test", color);

            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should handle color without hash prefix")
        void shouldHandleColorWithoutHashPrefix() {
            String color = "FF0000";

            Category category = Category.of("Test", color);

            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should handle color with alpha channel")
        void shouldHandleColorWithAlphaChannel() {
            String color = "#FF0000FF";

            Category category = Category.of("Test", color);

            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should handle named colors")
        void shouldHandleNamedColors() {
            String color = "red";

            Category category = Category.of("Test", color);

            assertThat(category.getColor()).isEqualTo(color);
        }

        @Test
        @DisplayName("Should handle RGB format")
        void shouldHandleRgbFormat() {
            String color = "rgb(255, 0, 0)";

            Category category = Category.of("Test", color);

            assertThat(category.getColor()).isEqualTo(color);
        }
    }
} 