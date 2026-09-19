package com.hms.ipd.repository;

import com.hms.ipd.entity.IpdRoundNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IpdRoundNoteRepository extends JpaRepository<IpdRoundNote, Long> {
    Page<IpdRoundNote> findByAdmissionIdOrderByRecordedAtDesc(Long admissionId, Pageable pageable);
}
