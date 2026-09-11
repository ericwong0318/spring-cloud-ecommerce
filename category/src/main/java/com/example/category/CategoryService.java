package com.example.category;

import com.example.common.dto.CategoryDto;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.ProductEvent;
import com.example.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper, OutboxEventPublisher outboxEventPublisher) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        log.debug("Fetching all categories");
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        log.debug("Fetching category by id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return categoryMapper.toDto(category);
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryTree(Long rootId) {
        log.debug("Fetching category tree for root: {}", rootId);
        Category root = categoryRepository.findByIdFetchChildren(rootId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", rootId));
        return buildCategoryTree(root);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getRootCategories() {
        log.debug("Fetching root categories");
        return categoryRepository.findRootsWithChildren().stream()
                .map(this::buildCategoryTree)
                .collect(Collectors.toList());
    }

    private CategoryDto buildCategoryTree(Category category) {
        CategoryDto dto = categoryMapper.toDto(category);
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            return new CategoryDto(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.parentId(),
                category.getChildren().stream()
                    .map(this::buildCategoryTree)
                    .collect(Collectors.toList())
            );
        }
        return new CategoryDto(
            dto.id(),
            dto.name(),
            dto.description(),
            dto.parentId(),
            List.of()
        );
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        log.info("Creating category: {}", categoryDto.name());
        Category category = categoryMapper.toEntity(categoryDto);
        
        if (categoryDto.parentId() != null) {
            Category parent = categoryRepository.findById(categoryDto.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", categoryDto.parentId()));
            category.setParent(parent);
        }
        
        Category saved = categoryRepository.save(category);
        publishCategoryEvent(ProductEvent.EventType.CATEGORY_CREATED, saved);
        return categoryMapper.toDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        log.info("Updating category id: {}", id);
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        existing.setName(categoryDto.name());
        existing.setDescription(categoryDto.description());

        Category saved = categoryRepository.save(existing);
        publishCategoryEvent(ProductEvent.EventType.CATEGORY_UPDATED, saved);
        return categoryMapper.toDto(saved);
    }

    @Transactional
    public CategoryDto moveSubtree(Long categoryId, Long newParentId) {
        log.info("Moving category {} under new parent {}", categoryId, newParentId);
        
        if (categoryId.equals(newParentId)) {
            throw new IllegalArgumentException("Cannot move category under itself");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        
        Category newParent = categoryRepository.findById(newParentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent Category", newParentId));

        // Check for cycles - category cannot be an ancestor of newParent (would create cycle)
        if (isAncestor(categoryId, newParentId)) {
            throw new IllegalArgumentException("Cannot move category under its own descendant (would create cycle)");
        }

        // Remove from current parent
        if (category.getParent() != null) {
            category.getParent().removeChild(category);
        }

        // Set new parent
        category.setParent(newParent);
        Category saved = categoryRepository.save(category);
        publishCategoryEvent(ProductEvent.EventType.CATEGORY_UPDATED, saved);
        return categoryMapper.toDto(saved);
    }

    private boolean isAncestor(Long potentialAncestor, Long descendant) {
        if (potentialAncestor.equals(descendant)) {
            return true;
        }
        List<Category> children = categoryRepository.findByParentId(potentialAncestor);
        for (Category child : children) {
            if (isAncestor(child.getId(), descendant)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void deleteWithCascade(Long categoryId) {
        log.info("Deleting category with cascade: {}", categoryId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        // Reparent children to parent (or make them root if no parent)
        Category parent = category.getParent();
        if (!category.getChildren().isEmpty()) {
            List<Category> children = new ArrayList<>(category.getChildren());
            for (Category child : children) {
                category.removeChild(child);
                if (parent != null) {
                    parent.addChild(child);
                }
            }
            categoryRepository.saveAll(children);
            if (parent != null) {
                categoryRepository.save(parent);
            }
        }

        publishCategoryEvent(ProductEvent.EventType.CATEGORY_DELETED, category);
        categoryRepository.delete(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category id: {}", id);
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id);
        }
        publishCategoryEvent(ProductEvent.EventType.CATEGORY_DELETED, categoryRepository.getReferenceById(id));
        categoryRepository.deleteById(id);
    }

    private void publishCategoryEvent(ProductEvent.EventType eventType, Category category) {
        ProductEvent event = switch (eventType) {
            case CATEGORY_CREATED -> ProductEvent.categoryCreated(category.getId(), category.getName(), category.getParent() != null ? category.getParent().getId() : null);
            case CATEGORY_UPDATED -> ProductEvent.categoryUpdated(category.getId(), category.getName(), category.getParent() != null ? category.getParent().getId() : null);
            case CATEGORY_DELETED -> ProductEvent.categoryDeleted(category.getId());
            default -> throw new IllegalArgumentException("Unsupported event type: " + eventType);
        };
        outboxEventPublisher.saveEvent("Category", category.getId().toString(), eventType.name(), event);
        log.info("Published ProductEvent.{} to outbox for category: {}", eventType, category.getId());
    }
}
