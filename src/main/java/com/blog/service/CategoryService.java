package com.blog.service;

import com.blog.dto.request.CategoryRequest;
import com.blog.dto.response.CategoryResponse;
import com.blog.exception.BadRequestException;
import com.blog.exception.ResourceNotFoundException;
import com.blog.model.Category;
import com.blog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public static CategoryResponse mapToResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(CategoryService::mapToResponse).toList();
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BadRequestException("Category already exists");
        }
        Category category = new Category(request.name(), request.description());
        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        
        categoryRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Category with this name already exists");
            }
        });

        category.setName(request.name());
        category.setDescription(request.description());
        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        categoryRepository.delete(category);
    }
}
