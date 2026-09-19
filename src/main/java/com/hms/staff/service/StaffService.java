package com.hms.staff.service;

import com.hms.common.codeseq.CodeGeneratorService;
import com.hms.common.exception.BusinessRuleException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.identity.entity.AppUser;
import com.hms.identity.entity.Role;
import com.hms.identity.repository.AppUserRepository;
import com.hms.staff.dto.StaffRequest;
import com.hms.staff.entity.StaffProfile;
import com.hms.staff.entity.StaffType;
import com.hms.staff.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class StaffService {

    private static final Map<StaffType, Role> ROLE_MAP = Map.of(
            StaffType.NURSE, Role.NURSE,
            StaffType.RECEPTIONIST, Role.RECEPTIONIST,
            StaffType.BILLING_STAFF, Role.BILLING_STAFF,
            StaffType.PHARMACIST, Role.PHARMACIST,
            StaffType.LAB_TECH, Role.LAB_TECH
    );

    private final StaffProfileRepository staffProfileRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CodeGeneratorService codeGeneratorService;

    public StaffProfile create(StaffRequest r) {
        if (r.username() == null || r.password() == null || r.fullName() == null) {
            throw new BusinessRuleException("username, password and fullName are required to create staff");
        }
        if (appUserRepository.existsByUsername(r.username())) {
            throw new DuplicateResourceException("Username '" + r.username() + "' is already taken");
        }
        AppUser user = new AppUser();
        user.setUsername(r.username());
        user.setEmail(r.email());
        user.setPasswordHash(passwordEncoder.encode(r.password()));
        user.setFullName(r.fullName());
        user.setPhone(r.phone());
        user.setRole(ROLE_MAP.get(r.staffType()));
        user = appUserRepository.save(user);

        StaffProfile profile = new StaffProfile();
        profile.setStaffCode(codeGeneratorService.next("STAFF", "ST", 5));
        profile.setUserId(user.getId());
        profile.setStaffType(r.staffType());
        profile.setDepartmentId(r.departmentId());
        profile.setShift(r.shift());
        profile.setJoiningDate(r.joiningDate());
        return staffProfileRepository.save(profile);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public StaffProfile getOrThrow(Long id) {
        return staffProfileRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Staff", id));
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<StaffProfile> search(StaffType type, String q, Pageable pageable) {
        return staffProfileRepository.search(type, q, pageable);
    }

    public void deactivate(Long id) {
        StaffProfile s = getOrThrow(id);
        s.setActive(false);
        staffProfileRepository.save(s);
    }
}
