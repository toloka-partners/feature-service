package com.sivalabs.ft.features.domain.entities;

import static org.junit.jupiter.api.Assertions.*;

import com.sivalabs.ft.features.domain.models.DependencyType;
import com.sivalabs.ft.features.domain.models.FeatureStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeatureDependencyTest {

    private Feature feature1;
    private Feature feature2;
    private FeatureDependency dependency;

    @BeforeEach
    void setUp() {
        feature1 = createFeature("FEAT-001", "Feature 1");
        feature2 = createFeature("FEAT-002", "Feature 2");
        dependency = new FeatureDependency();
    }

    @Test
    void testFeatureDependencyCreation() {
        dependency.setFeature(feature1);
        dependency.setDependsOnFeature(feature2);
        dependency.setDependencyType(DependencyType.HARD);
        dependency.setNotes("Feature 1 requires Feature 2 to be completed first");

        assertEquals(feature1, dependency.getFeature());
        assertEquals(feature2, dependency.getDependsOnFeature());
        assertEquals(DependencyType.HARD, dependency.getDependencyType());
        assertEquals("Feature 1 requires Feature 2 to be completed first", dependency.getNotes());
    }

    @Test
    void testAllDependencyTypes() {
        dependency.setDependencyType(DependencyType.HARD);
        assertEquals(DependencyType.HARD, dependency.getDependencyType());

        dependency.setDependencyType(DependencyType.SOFT);
        assertEquals(DependencyType.SOFT, dependency.getDependencyType());

        dependency.setDependencyType(DependencyType.OPTIONAL);
        assertEquals(DependencyType.OPTIONAL, dependency.getDependencyType());
    }

    @Test
    void testCreatedAtIsSetAutomatically() {
        Instant before = Instant.now();
        dependency.onCreate();
        Instant after = Instant.now();

        assertNotNull(dependency.getCreatedAt());
        assertTrue(dependency.getCreatedAt().compareTo(before) >= 0);
        assertTrue(dependency.getCreatedAt().compareTo(after) <= 0);
    }

    @Test
    void testCreatedAtNotOverwrittenIfAlreadySet() {
        Instant customTime = Instant.parse("2024-01-01T00:00:00Z");
        dependency.setCreatedAt(customTime);
        dependency.onCreate();

        assertEquals(customTime, dependency.getCreatedAt());
    }

    @Test
    void testNotesCanBeNullOrEmpty() {
        dependency.setNotes(null);
        assertNull(dependency.getNotes());

        dependency.setNotes("");
        assertEquals("", dependency.getNotes());

        dependency.setNotes("Some notes");
        assertEquals("Some notes", dependency.getNotes());
    }

    @Test
    void testIdGetterAndSetter() {
        assertNull(dependency.getId());

        dependency.setId(1L);
        assertEquals(1L, dependency.getId());
    }

    @Test
    void testBidirectionalRelationships() {
        feature1.getDependencies().add(dependency);
        feature2.getDependents().add(dependency);

        dependency.setFeature(feature1);
        dependency.setDependsOnFeature(feature2);

        assertTrue(feature1.getDependencies().contains(dependency));
        assertTrue(feature2.getDependents().contains(dependency));
    }

    private Feature createFeature(String code, String title) {
        Feature feature = new Feature();
        feature.setCode(code);
        feature.setTitle(title);
        feature.setStatus(FeatureStatus.NEW);
        feature.setCreatedBy("test-user");
        feature.setCreatedAt(Instant.now());
        return feature;
    }
}
