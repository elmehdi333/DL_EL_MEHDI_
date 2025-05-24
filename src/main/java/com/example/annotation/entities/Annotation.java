package com.example.annotation.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text1;
    private String text2;
    private String annotationValue;
    private String status;

    @Column(nullable = true)
    private Integer progressPercentage;

    @Column
    private Boolean similar; // true = similar, false = not similar, null = not set

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    private Dataset dataset;

    @ManyToOne
    private User assignedTo;

    // === Constructor ===
    public Annotation() {
        this.status = "unlabeled";
        this.progressPercentage = 0;
    }

    // === Getters and Setters ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText1() {
        return text1;
    }

    public void setText1(String text1) {
        this.text1 = text1;
    }

    public String getText2() {
        return text2;
    }

    public void setText2(String text2) {
        this.text2 = text2;
    }

    public String getAnnotationValue() {
        return annotationValue;
    }

    public void setAnnotationValue(String annotationValue) {
        this.annotationValue = annotationValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgressPercentage() {
        if ("labeled".equalsIgnoreCase(status)) {
            return 100;
        } else if ("unlabeled".equalsIgnoreCase(status)) {
            return 0;
        }
        return progressPercentage != null ? progressPercentage : 0;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Boolean getSimilar() {
        return similar;
    }

    public void setSimilar(Boolean similar) {
        this.similar = similar;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }
}
