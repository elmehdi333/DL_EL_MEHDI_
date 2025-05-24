package com.example.annotation.repositories;

import com.example.annotation.entities.Annotation;
import com.example.annotation.entities.Dataset;
import com.example.annotation.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnotationRepository extends JpaRepository<Annotation, Long> {

    // Annotations assigned to a specific user
    List<Annotation> findByAssignedTo(User user);
    List<Annotation> findByDatasetAndStatusIgnoreCase(Dataset dataset, String status);

    // All annotations for a dataset
    List<Annotation> findByDataset(Dataset dataset);

    // Paginated annotations for a dataset
    Page<Annotation> findByDataset(Dataset dataset, Pageable pageable);

    // Paginated annotations for a dataset filtered by status (labeled, in-progress, etc.)
    Page<Annotation> findByDatasetAndStatusIgnoreCase(Dataset dataset, String status, Pageable pageable);

    // Count annotations that are not unlabeled
    long countByDatasetAndStatusNotIgnoreCase(Dataset dataset, String status);

    // Count total annotations
    long countByDataset(Dataset dataset);

    // All annotations in dataset that are unassigned
    List<Annotation> findByDatasetAndAssignedToIsNull(Dataset dataset);
}
