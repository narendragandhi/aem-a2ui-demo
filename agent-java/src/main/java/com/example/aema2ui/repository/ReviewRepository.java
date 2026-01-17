package com.example.aema2ui.repository;

import com.example.aema2ui.model.ContentVersion;
import com.example.aema2ui.model.Review;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for reviews and content versions.
 * Uses ConcurrentHashMap for thread-safety.
 * Can be replaced with JPA repository for production.
 */
@Repository
public class ReviewRepository {

    private final Map<String, Review> reviews = new ConcurrentHashMap<>();
    private final Map<String, List<ContentVersion>> contentVersions = new ConcurrentHashMap<>();

    // Review operations

    public Review save(Review review) {
        reviews.put(review.getId(), review);
        return review;
    }

    public Optional<Review> findById(String id) {
        return Optional.ofNullable(reviews.get(id));
    }

    public List<Review> findByContentId(String contentId) {
        return reviews.values().stream()
                .filter(r -> contentId.equals(r.getContentId()))
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Review> findAll() {
        return new ArrayList<>(reviews.values());
    }

    public void deleteById(String id) {
        reviews.remove(id);
    }

    public boolean existsById(String id) {
        return reviews.containsKey(id);
    }

    // Content version operations

    public ContentVersion saveVersion(ContentVersion version) {
        contentVersions.computeIfAbsent(version.getContentId(), k -> new ArrayList<>())
                .add(version);
        return version;
    }

    public List<ContentVersion> findVersionsByContentId(String contentId) {
        return contentVersions.getOrDefault(contentId, Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(ContentVersion::getVersion).reversed())
                .collect(Collectors.toList());
    }

    public Optional<ContentVersion> findVersionByContentIdAndVersion(String contentId, int version) {
        return contentVersions.getOrDefault(contentId, Collections.emptyList())
                .stream()
                .filter(v -> v.getVersion() == version)
                .findFirst();
    }

    public int getNextVersionNumber(String contentId) {
        return contentVersions.getOrDefault(contentId, Collections.emptyList()).size() + 1;
    }

    // Utility methods

    public void clear() {
        reviews.clear();
        contentVersions.clear();
    }
}
