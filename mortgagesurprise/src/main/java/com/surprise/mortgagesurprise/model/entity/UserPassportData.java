package com.surprise.mortgagesurprise.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "user_passport_data",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_passport_user", columnNames = {"user_id"}),
        @UniqueConstraint(name = "uq_user_passport_hash", columnNames = {"passport_hash"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class UserPassportData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "series_number_encrypted", nullable = false, length = 512)
    private String seriesNumberEncrypted;

    @Column(name = "passport_hash", nullable = false, length = 64)
    private String passportHash;

    @Column(name = "issue_date", length = 32)
    private String issueDate;

    @Column(name = "department_code", length = 32)
    private String departmentCode;

    @Column(name = "issued_by", length = 255)
    private String issuedBy;

    @Column(name = "age")
    private Integer age;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "gender", length = 32)
    private String gender;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
