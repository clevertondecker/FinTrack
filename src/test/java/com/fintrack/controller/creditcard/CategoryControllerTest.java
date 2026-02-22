package com.fintrack.controller.creditcard;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.application.creditcard.CategoryService;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.dto.creditcard.CategoryCreateRequest;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CategoryController Tests")
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.of("Food", "#FF0000");
    }

    @Nested
    @DisplayName("GET /api/categories Tests")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("Should return all categories successfully")
        void shouldReturnAllCategoriesSuccessfully() throws Exception {
            List<Category> categories = List.of(testCategory);
            when(categoryService.findAll()).thenReturn(categories);

            mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Categories retrieved successfully"))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories[0].id").value(testCategory.getId()))
                .andExpect(jsonPath("$.categories[0].name").value("Food"))
                .andExpect(jsonPath("$.categories[0].color").value("#FF0000"))
                .andExpect(jsonPath("$.count").value(1));
        }

        @Test
        @DisplayName("Should return empty list when no categories exist")
        void shouldReturnEmptyListWhenNoCategoriesExist() throws Exception {
            when(categoryService.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Categories retrieved successfully"))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories").isEmpty())
                .andExpect(jsonPath("$.count").value(0));
        }

        @Test
        @DisplayName("Should return multiple categories")
        void shouldReturnMultipleCategories() throws Exception {
            Category category1 = Category.of("Food", "#FF0000");
            Category category2 = Category.of("Transport", "#00FF00");
            Category category3 = Category.of("Entertainment", "#0000FF");

            List<Category> categories = List.of(category1, category2, category3);
            when(categoryService.findAll()).thenReturn(categories);

            mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Categories retrieved successfully"))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(3))
                .andExpect(jsonPath("$.categories[0].name").value("Food"))
                .andExpect(jsonPath("$.categories[1].name").value("Transport"))
                .andExpect(jsonPath("$.categories[2].name").value("Entertainment"))
                .andExpect(jsonPath("$.count").value(3));
        }
    }

    @Nested
    @DisplayName("POST /api/categories Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create category successfully")
        void shouldCreateCategorySuccessfully() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("Food", "#FF0000");
            when(categoryService.create(any(), any())).thenReturn(testCategory);

            mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.id").value(testCategory.getId()))
                .andExpect(jsonPath("$.name").value("Food"))
                .andExpect(jsonPath("$.color").value("#FF0000"));
        }

        @Test
        @DisplayName("Should create category without color")
        void shouldCreateCategoryWithoutColor() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("Food", null);
            when(categoryService.create(any(), any())).thenReturn(testCategory);

            mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.id").value(testCategory.getId()))
                .andExpect(jsonPath("$.name").value("Food"));
        }

        @Test
        @DisplayName("Should return error when name is null")
        void shouldReturnErrorWhenNameIsNull() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest(null, "#FF0000");

            mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Category name is required"));
        }

        @Test
        @DisplayName("Should return error when name is empty")
        void shouldReturnErrorWhenNameIsEmpty() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("", "#FF0000");

            mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Category name is required"));
        }

        @Test
        @DisplayName("Should return error when name is blank")
        void shouldReturnErrorWhenNameIsBlank() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("   ", "#FF0000");

            mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Category name is required"));
        }

        @Test
        @DisplayName("Should return error when color is too long")
        void shouldReturnErrorWhenColorIsTooLong() throws Exception {
            String longColor = "This color is way too long for the validation";
            CategoryCreateRequest request = new CategoryCreateRequest("Food", longColor);

            mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Color must be at most 20 characters"));
        }

        @Test
        @DisplayName("Should trim whitespace from name")
        void shouldTrimWhitespaceFromName() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("  Food  ", "#FF0000");
            when(categoryService.create(any(), any())).thenReturn(testCategory);

            mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.name").value("Food"));
        }

        @Test
        @DisplayName("Should handle special characters in name")
        void shouldHandleSpecialCharactersInName() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("Food & Drinks", "#FF0000");
            Category specialCategory = Category.of("Food & Drinks", "#FF0000");
            when(categoryService.create(any(), any())).thenReturn(specialCategory);

            mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.name").value("Food & Drinks"));
        }

        @Test
        @DisplayName("Should handle maximum allowed category name length")
        void shouldHandleMaximumAllowedCategoryNameLength() throws Exception {
          String maxLengthName;
          maxLengthName = "A".repeat(50);
          CategoryCreateRequest request = new CategoryCreateRequest(maxLengthName, "#FF0000");
            Category longCategory = Category.of(maxLengthName, "#FF0000");
            when(categoryService.create(any(), any())).thenReturn(longCategory);

            mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.name").value(maxLengthName));
        }

        @Test
        @DisplayName("Should handle minimum allowed category name length")
        void shouldHandleMinimumAllowedCategoryNameLength() throws Exception {
            CategoryCreateRequest request = new CategoryCreateRequest("AB", "#FF0000");
            Category minCategory = Category.of("AB", "#FF0000");
            when(categoryService.create(any(), any())).thenReturn(minCategory);

            mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category created successfully"))
                .andExpect(jsonPath("$.name").value("AB"));
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create CategoryController with valid dependencies")
        void shouldCreateCategoryControllerWithValidDependencies() {
            CategoryController controller = new CategoryController(categoryService);
            assertNotNull(controller);
        }
    }
}