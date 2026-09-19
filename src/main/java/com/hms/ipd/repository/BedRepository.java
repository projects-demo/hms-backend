package com.hms.ipd.repository;

import com.hms.ipd.entity.Bed;
import com.hms.ipd.entity.BedStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BedRepository extends JpaRepository<Bed, Long> {

    @Query("select b from Bed b join fetch b.ward where b.status = :status and (:wardId is null or b.ward.id = :wardId)")
    List<Bed> findAvailable(@Param("status") BedStatus status, @Param("wardId") Long wardId);

    @Query("select b from Bed b join fetch b.ward where (:wardId is null or b.ward.id = :wardId)")
    Page<Bed> search(@Param("wardId") Long wardId, Pageable pageable);

    long countByWardIdAndStatus(Long wardId, BedStatus status);

    long countByStatus(BedStatus status);
}
