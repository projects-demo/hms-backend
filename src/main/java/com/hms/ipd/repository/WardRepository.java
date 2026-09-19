package com.hms.ipd.repository;

import com.hms.ipd.entity.Ward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardRepository extends JpaRepository<Ward, Long> {
    Page<Ward> findByNameContainingIgnoreCase(String q, Pageable pageable);
}
