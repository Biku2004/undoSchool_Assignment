package com.spring.coursesearch.services;

import com.spring.coursesearch.entity.CourseDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class DataLoaderService {

    @Autowired
    private ElasticsearchOperations elasticsearchTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void loadSampleData() throws IOException {

        // Delete and recreate the index to ensure a clean state
        elasticsearchTemplate.indexOps(CourseDocument.class).delete();
        elasticsearchTemplate.indexOps(CourseDocument.class).create();

        // Read sample-courses.json
        ClassPathResource resource = new ClassPathResource("sample-courses.json");
        List<CourseDocument> courses = objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});

        // Bulk index
        courses.forEach(course -> elasticsearchTemplate.save(course));
    }
}