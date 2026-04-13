package com.courtier.courtier.case_.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "case_acts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseAct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case courtCase;

    private String actName;
    private String section;
}
