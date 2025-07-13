package com.spring.coursesearch.services;

import com.spring.coursesearch.entity.CourseDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public CourseSearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public SearchHits<CourseDocument> searchCourses(String query, Integer minAge, Integer maxAge, String category,
                                                    String type, Double minPrice, Double maxPrice, String startDate,
                                                    String sort, int page, int size) {
        Criteria criteria = new Criteria();

        // Full-text search on title and description with fuzziness on title
        if (query != null && !query.isEmpty()) {
            Criteria titleCriteria = new Criteria("title").fuzzy(query); // Enable fuzzy matching
            Criteria descCriteria = new Criteria("description").matches(query); // Standard match for description
            criteria = titleCriteria.or(descCriteria); // Combine both criteria
        }

        // Range filters
        if (minAge != null) {
            criteria = criteria.and(new Criteria("minAge").greaterThanEqual(minAge));
        }
        if (maxAge != null) {
            criteria = criteria.and(new Criteria("maxAge").lessThanEqual(maxAge));
        }
        if (minPrice != null) {
            criteria = criteria.and(new Criteria("price").greaterThanEqual(minPrice));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("price").lessThanEqual(maxPrice));
        }

        // Exact filters
        if (category != null && !category.isEmpty()) {
            criteria = criteria.and(new Criteria("category").is(category));
        }
        if (type != null && !type.isEmpty()) {
            criteria = criteria.and(new Criteria("type").is(type));
        }

        // Date filter
        if (startDate != null && !startDate.isEmpty()) {
            criteria = criteria.and(new Criteria("nextSessionDate").greaterThanEqual(startDate));
        }

        // Sorting
        Sort sortBy;
        if ("priceAsc".equalsIgnoreCase(sort)) {
            sortBy = Sort.by(Sort.Direction.ASC, "price");
        } else if ("priceDesc".equalsIgnoreCase(sort)) {
            sortBy = Sort.by(Sort.Direction.DESC, "price");
        } else {
            sortBy = Sort.by(Sort.Direction.ASC, "nextSessionDate");
        }

        CriteriaQuery searchQuery = new CriteriaQuery(criteria, PageRequest.of(page, size)).addSort(sortBy);

        return elasticsearchOperations.search(searchQuery, CourseDocument.class);
    }

    public List<String> suggestTitles(String partialTitle) {
        if (partialTitle == null || partialTitle.isEmpty()) {
            return List.of();
        }
        Criteria criteria = new Criteria("title").startsWith(partialTitle); // Prefix match for autocomplete
        CriteriaQuery query = new CriteriaQuery(criteria, PageRequest.of(0, 10));
        SearchHits<CourseDocument> searchHits = elasticsearchOperations.search(query, CourseDocument.class);
        return searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent().getTitle())
                .distinct() // Ensure no duplicates
                .collect(Collectors.toList());
    }
}

