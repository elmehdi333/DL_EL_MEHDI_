package com.example.annotation.repositories;

import com.example.annotation.entities.Dataset;
import com.example.annotation.entities.User;

import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    List<Dataset> findByCreatedBy(User user);
//    Page<Annotation> findByDataset(Dataset dataset, Pageable pageable);

}
