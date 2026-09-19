package com.hms.common.codeseq;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mints sequential, human-readable business codes (PT-000123, APT-000042, ...)
 * per hospital schema. Uses a row-level pessimistic lock on a tiny counter
 * table so concurrent requests never collide - safer under load than
 * "SELECT MAX(id)+1" and independent of MySQL's AUTO_INCREMENT gap behavior.
 * REQUIRES_NEW so a failed parent transaction never rolls back (and thus
 * never reuses) an already-issued code.
 */
@Service
@RequiredArgsConstructor
public class CodeGeneratorService {

    private final CodeSequenceRepository repository;

    @Transactional(value = "transactionManager", propagation = Propagation.REQUIRES_NEW)
    public String next(String entityType, String prefix, int padding) {
        CodeSequence seq = repository.lockByType(entityType)
                .orElseGet(() -> {
                    CodeSequence s = new CodeSequence();
                    s.setEntityType(entityType);
                    s.setLastValue(0L);
                    return repository.save(s);
                });
        long nextVal = seq.getLastValue() + 1;
        seq.setLastValue(nextVal);
        repository.save(seq);
        return prefix + "-" + String.format("%0" + padding + "d", nextVal);
    }
}
