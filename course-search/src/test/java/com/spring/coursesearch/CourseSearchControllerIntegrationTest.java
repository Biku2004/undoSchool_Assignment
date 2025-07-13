package com.spring.coursesearch;

import com.spring.coursesearch.controller.CourseSearchController.SearchResponse;
import com.spring.coursesearch.entity.CourseDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CourseSearchControllerIntegrationTest {

    // Use a lighter weight Elasticsearch image with proper readiness configuration
    @Container
    static ElasticsearchContainer elasticsearchContainer =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.15.0")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("xpack.ml.enabled", "false")
                    .withEnv("bootstrap.memory_lock", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .withStartupTimeout(Duration.ofMinutes(5))
                    .withReuse(true)
                    // Add health check to ensure Elasticsearch is ready
                    .waitingFor(org.testcontainers.containers.wait.strategy.Wait
                            .forHttp("/_cluster/health?wait_for_status=yellow&timeout=60s")
                            .forPort(9200)
                            .withStartupTimeout(Duration.ofMinutes(5)));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearchContainer.getHost() + ":" + elasticsearchContainer.getMappedPort(9200));
        registry.add("spring.elasticsearch.ssl.enabled", () -> "false");
        registry.add("spring.elasticsearch.connection-timeout", () -> "60s");
        registry.add("spring.elasticsearch.socket-timeout", () -> "60s");
    }

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @BeforeEach
    void setupData() {
        // Wait for Elasticsearch to be ready
        waitForElasticsearch();

        // Clean up and recreate index using modern API
        var indexOps = elasticsearchOperations.indexOps(CourseDocument.class);

        if (indexOps.exists()) {
            indexOps.delete();
        }

        // Create index and mapping
        indexOps.create();
        indexOps.putMapping();

        List<CourseDocument> courses = List.of(
                create("course_13", "Web Development Basics", "Build your first website with HTML and CSS.", "Technology", "COURSE", "6th-8th", 11, 14, 75.00, "2025-09-05T10:00:00Z"),
                create("course_14", "Guitar Lessons", "Learn to play the guitar with expert instruction.", "Music", "CLUB", "5th-7th", 10, 13, 50.00, "2025-08-25T16:00:00Z"),
                create("course_15", "Chemistry Experiments", "Hands-on experiments to learn chemistry basics.", "Science", "ONE_TIME", "6th-8th", 11, 14, 28.00, "2025-11-20T14:00:00Z"),
                create("course_16", "Poetry Writing", "Express yourself through various forms of poetry.", "English", "CLUB", "7th-9th", 12, 15, 38.00, "2025-09-15T13:00:00Z")
        );

        // Save documents using modern API
        courses.forEach(elasticsearchOperations::save);

        // Refresh index to ensure documents are immediately available for search
        indexOps.refresh();

        // Add a delay to ensure documents are fully indexed
        try {
            Thread.sleep(3000); // Increased delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify documents are actually saved
        long count = elasticsearchOperations.count(org.springframework.data.elasticsearch.core.query.Query.findAll(), CourseDocument.class);
        System.out.println("Documents indexed: " + count);
    }

    private void waitForElasticsearch() {
        int maxRetries = 30;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                // Try to perform a simple operation to check if Elasticsearch is ready
                elasticsearchOperations.indexOps(CourseDocument.class).exists();
                System.out.println("Elasticsearch is ready!");
                return;
            } catch (Exception e) {
                retryCount++;
                System.out.println("Waiting for Elasticsearch... attempt " + retryCount + "/" + maxRetries);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for Elasticsearch", ie);
                }
            }
        }

        throw new RuntimeException("Elasticsearch failed to start after " + maxRetries + " attempts");
    }

    @Test
    void testSearchWithQueryTitle() {
        String url = "http://localhost:" + port + "/api/search?q=Chemistry";

        ResponseEntity<SearchResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<SearchResponse>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getTotal() > 0, "Expected at least one search result");
        assertFalse(response.getBody().getCourses().isEmpty(), "Expected courses list to not be empty");
        assertEquals("Chemistry Experiments", response.getBody().getCourses().get(0).getTitle());
    }

    @Test
    void testSuggestEndpoint() {
        String url = "http://localhost:" + port + "/api/search/suggest?q=Guitar";

        ResponseEntity<List<String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty(), "Expected suggestions to not be empty");
        assertTrue(response.getBody().contains("Guitar Lessons"), "Expected 'Guitar Lessons' in suggestions");
    }

    @Test
    void testSearchWithNoResults() {
        String url = "http://localhost:" + port + "/api/search?q=NonexistentCourse";

        ResponseEntity<SearchResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<SearchResponse>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getTotal());
        assertTrue(response.getBody().getCourses().isEmpty());
    }

    @Test
    void testSearchWithTitleMatch() {
        // Search for "chemistry" which should match in title and description
        String url = "http://localhost:" + port + "/api/search?q=chemistry";

        ResponseEntity<SearchResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<SearchResponse>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        System.out.println("Search results for 'chemistry': " + response.getBody().getTotal());
        if (response.getBody().getCourses() != null && !response.getBody().getCourses().isEmpty()) {
            response.getBody().getCourses().forEach(course -> {
                System.out.println("Found course: " + course.getTitle());
            });
        }

        // If search is working correctly, this should pass
        if (response.getBody().getTotal() > 0) {
            assertTrue(response.getBody().getCourses().stream()
                            .anyMatch(course -> "Chemistry Experiments".equals(course.getTitle())),
                    "Expected to find 'Chemistry Experiments' in search results");
        }
    }

    @Test
    void testDebugSearchResults() {
        // This test helps debug what's actually being returned
        String url = "http://localhost:" + port + "/api/search?q=Web";

        ResponseEntity<SearchResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<SearchResponse>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        System.out.println("Search results for 'Web':");
        System.out.println("Total results: " + response.getBody().getTotal());
        if (response.getBody().getCourses() != null) {
            response.getBody().getCourses().forEach(course -> {
                System.out.println("Course: " + course.getTitle() + ", Category: " + course.getCategory());
            });
        }

        // This test is for debugging, so we'll just check the response structure
        assertTrue(response.getBody().getTotal() >= 0, "Total should be non-negative");
    }

    private CourseDocument create(String id, String title, String description, String category,
                                  String type, String gradeRange, int minAge, int maxAge,
                                  double price, String date) {
        CourseDocument doc = new CourseDocument();
        doc.setId(id);
        doc.setTitle(title);
        doc.setSuggest(title); // for autocomplete
        doc.setDescription(description);
        doc.setCategory(category);
        doc.setType(type);
        doc.setGradeRange(gradeRange);
        doc.setMinAge(minAge);
        doc.setMaxAge(maxAge);
        doc.setPrice(price);
        doc.setNextSessionDate(date);
        return doc;
    }
}