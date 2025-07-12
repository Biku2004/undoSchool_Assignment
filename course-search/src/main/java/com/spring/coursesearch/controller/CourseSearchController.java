package com.spring.coursesearch.controller;

import com.spring.coursesearch.entity.CourseDocument;
import com.spring.coursesearch.services.CourseSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class CourseSearchController {

    @Autowired
    private CourseSearchService searchService;

    @GetMapping
    public SearchResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String startDate,
            @RequestParam(defaultValue = "upcoming") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        SearchHits<CourseDocument> searchHits = searchService.searchCourses(
                q, minAge, maxAge, category, type, minPrice, maxPrice, startDate, sort, page, size);

        List<CourseSummary> courses = searchHits.getSearchHits().stream()
                .map(hit -> {
                    CourseDocument doc = hit.getContent();
                    return new CourseSummary(doc.getId(), doc.getTitle(), doc.getCategory(), doc.getPrice(), doc.getNextSessionDate());
                })
                .collect(Collectors.toList());

        return new SearchResponse(searchHits.getTotalHits(), courses);
    }

    public static class SearchResponse {
        private final long total;
        private final List<CourseSummary> courses;

        public SearchResponse(long total, List<CourseSummary> courses) {
            this.total = total;
            this.courses = courses;
        }

        // Getters
        public long getTotal() {
            return total;
        }

        public List<CourseSummary> getCourses() {
            return courses;
        }
    }

    public static class CourseSummary {
        private final String id;
        private final String title;
        private final String category;
        private final Double price;
        private final String nextSessionDate;

        public CourseSummary(String id, String title, String category, Double price, String nextSessionDate) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.price = price;
            this.nextSessionDate = nextSessionDate;
        }

        // Getters
        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getCategory() {
            return category;
        }

        public Double getPrice() {
            return price;
        }

        public String getNextSessionDate() {
            return nextSessionDate;
        }
    }
}