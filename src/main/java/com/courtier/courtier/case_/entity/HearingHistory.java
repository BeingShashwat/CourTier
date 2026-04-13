package com.courtier.courtier.case_.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "hearing_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HearingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case courtCase;

    private LocalDate hearingDate;
    private String purpose;
    private String judgeName;
}
