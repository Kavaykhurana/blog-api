package com.blog.service;

import com.blog.dto.request.TagRequest;
import com.blog.dto.response.TagResponse;
import com.blog.exception.BadRequestException;
import com.blog.exception.ResourceNotFoundException;
import com.blog.model.Tag;
import com.blog.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public static TagResponse mapToResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream().map(TagService::mapToResponse).toList();
    }

    public TagResponse createTag(TagRequest request) {
        String cleanName = request.name().replaceAll("#", "").trim();
        if (cleanName.isEmpty()) {
            throw new BadRequestException("Tag name cannot be empty");
        }
        if (tagRepository.existsByNameIgnoreCase(cleanName)) {
            throw new BadRequestException("Tag already exists");
        }
        Tag tag = new Tag(cleanName);
        Tag saved = tagRepository.save(tag);
        return mapToResponse(saved);
    }

    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));
        tagRepository.delete(tag);
    }
}
