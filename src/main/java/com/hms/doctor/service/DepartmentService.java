package com.hms.doctor.service;

import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.doctor.dto.DepartmentRequest;
import com.hms.doctor.entity.Department;
import com.hms.doctor.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public Department create(DepartmentRequest r) {
        departmentRepository.findAll().stream()
                .filter(d -> d.getName().equalsIgnoreCase(r.name()))
                .findAny()
                .ifPresent(d -> { throw new DuplicateResourceException("Department '" + r.name() + "' already exists"); });
        Department d = new Department();
        d.setName(r.name());
        d.setDescription(r.description());
        return departmentRepository.save(d);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Department> list(String q, Pageable pageable) {
        return (q == null || q.isBlank()) ? departmentRepository.findAll(pageable)
                : departmentRepository.findByNameContainingIgnoreCase(q.trim(), pageable);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Department getOrThrow(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }
}
