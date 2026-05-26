package com.courtier.courtier.case_.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cnrNumber;

    private String caseType;
    private String filingNumber;
    private LocalDate filingDate;
    private String registrationNumber;
    private LocalDate registrationDate;

    private String courtName;
    private String courtNumber;
    private String judgeName;

    private String petitionerName;
    private String respondentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    private LocalDate nextHearingDate;
    private LocalDate lastHearingDate;
    private String caseStage;

    private LocalDateTime lastPolledAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "courtCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HearingHistory> hearingHistory = new ArrayList<>();

    @OneToMany(mappedBy = "courtCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CaseAct> acts = new ArrayList<>();

    @OneToMany(mappedBy = "courtCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserCase> trackedBy = new ArrayList<>();

    public enum CaseStatus {
        PENDING, DISPOSED, TRANSFERRED, UNKNOWN
    }
}
