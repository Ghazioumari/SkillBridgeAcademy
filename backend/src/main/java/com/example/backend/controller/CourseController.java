package com.example.backend.controller;

import com.example.backend.model.Course;
import com.example.backend.service.CourseService;
import com.example.backend.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "http://localhost:4200")
public class CourseController {
    
    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    @Autowired
    private CourseService courseService;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "Please select a file to upload"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "Only image files are allowed"));
            }

            logger.info("Starting file upload: {}", file.getOriginalFilename());
            String fileName = fileStorageService.storeFile(file);
            logger.info("File uploaded successfully: {}", fileName);
            
            return ResponseEntity.ok(Collections.singletonMap("fileName", fileName));
        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Could not upload the file: " + e.getMessage()));
        }
    }

    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            logger.info("Fetching image: {}", fileName);
            Path filePath = fileStorageService.getFilePath(fileName);
            
            if (!Files.exists(filePath)) {
                logger.warn("Image file not found: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            
        } catch (IOException e) {
            logger.error("Error fetching image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        try {
            logger.info("Fetching all courses");
            List<Course> courses = courseService.getAllCourses();
            logger.info("Found {} courses", courses.size());
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            logger.error("Error fetching courses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        try {
            logger.info("Fetching course with id: {}", id);
            return courseService.getCourseById(id)
                    .map(course -> {
                        logger.info("Found course: {}", course);
                        return ResponseEntity.ok(course);
                    })
                    .orElseGet(() -> {
                        logger.warn("Course not found with id: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Error fetching course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        try {
            logger.info("Creating new course: {}", course);
            Course savedCourse = courseService.saveCourse(course);
            logger.info("Course created successfully: {}", savedCourse);
            return ResponseEntity.ok(savedCourse);
        } catch (Exception e) {
            logger.error("Error creating course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        try {
            logger.info("Updating course with id: {}", id);
            return courseService.getCourseById(id)
                    .map(existingCourse -> {
                        course.setId(id);
                        Course updatedCourse = courseService.saveCourse(course);
                        logger.info("Course updated successfully: {}", updatedCourse);
                        return ResponseEntity.ok(updatedCourse);
                    })
                    .orElseGet(() -> {
                        logger.warn("Course not found with id: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Error updating course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        try {
            logger.info("Deleting course with id: {}", id);
            return courseService.getCourseById(id)
                    .map(course -> {
                        courseService.deleteCourse(id);
                        logger.info("Course deleted successfully: {}", id);
                        return ResponseEntity.ok().<Void>build();
                    })
                    .orElseGet(() -> {
                        logger.warn("Course not found with id: {}", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.error("Error deleting course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
