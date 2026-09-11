package com.example.category;

import com.example.common.dto.CategoryDto;
import com.example.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryDto> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/tree")
    public List<CategoryDto> getCategoryTree() {
        return categoryService.getRootCategories();
    }

    @GetMapping("/tree/{rootId}")
    public ResponseEntity<CategoryDto> getCategoryTreeByRoot(@PathVariable Long rootId) {
        return ResponseEntity.ok(categoryService.getCategoryTree(rootId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        CategoryDto created = categoryService.createCategory(categoryDto);
        return ResponseEntity.created(URI.create("/categories/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDto categoryDto) {
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryDto));
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<CategoryDto> moveSubtree(
            @PathVariable Long id,
            @RequestParam Long newParentId) {
        return ResponseEntity.ok(categoryService.moveSubtree(id, newParentId));
    }

    @DeleteMapping("/{id}/cascade")
    public ResponseEntity<Void> deleteWithCascade(@PathVariable Long id) {
        categoryService.deleteWithCascade(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }
}
