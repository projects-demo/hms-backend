package com.hms.common.codeseq;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "code_sequences")
public class CodeSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, unique = true, length = 50)
    private String entityType;

    @Column(name = "seq_value", nullable = false)
    private Long lastValue = 0L;
}
