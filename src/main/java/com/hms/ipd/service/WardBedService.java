package com.hms.ipd.service;

import com.hms.common.exception.BusinessRuleException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.ipd.dto.BedRequest;
import com.hms.ipd.dto.WardRequest;
import com.hms.ipd.entity.Bed;
import com.hms.ipd.entity.BedStatus;
import com.hms.ipd.entity.Ward;
import com.hms.ipd.repository.BedRepository;
import com.hms.ipd.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional("transactionManager")
public class WardBedService {

    private final WardRepository wardRepository;
    private final BedRepository bedRepository;

    public Ward createWard(WardRequest r) {
        Ward w = new Ward();
        w.setName(r.name());
        w.setWardType(r.wardType());
        w.setFloor(r.floor());
        w.setTotalBeds(0);
        return wardRepository.save(w);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Ward> listWards(String q, Pageable pageable) {
        return (q == null || q.isBlank()) ? wardRepository.findAll(pageable) : wardRepository.findByNameContainingIgnoreCase(q, pageable);
    }

    public Bed createBed(BedRequest r) {
        Ward ward = wardRepository.findById(r.wardId()).orElseThrow(() -> ResourceNotFoundException.of("Ward", r.wardId()));
        Bed bed = new Bed();
        bed.setWard(ward);
        bed.setBedNumber(r.bedNumber());
        bed.setDailyRate(r.dailyRate());
        bed.setStatus(BedStatus.AVAILABLE);
        bed = bedRepository.save(bed);
        ward.setTotalBeds(ward.getTotalBeds() + 1);
        wardRepository.save(ward);
        return bed;
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public List<Bed> availableBeds(Long wardId) {
        return bedRepository.findAvailable(BedStatus.AVAILABLE, wardId);
    }

    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Bed> searchBeds(Long wardId, Pageable pageable) {
        return bedRepository.search(wardId, pageable);
    }

    public Bed setMaintenance(Long bedId, boolean underMaintenance) {
        Bed bed = bedRepository.findById(bedId).orElseThrow(() -> ResourceNotFoundException.of("Bed", bedId));
        if (bed.getStatus() == BedStatus.OCCUPIED && underMaintenance) {
            throw new BusinessRuleException("Cannot mark an occupied bed for maintenance");
        }
        bed.setStatus(underMaintenance ? BedStatus.MAINTENANCE : BedStatus.AVAILABLE);
        return bedRepository.save(bed);
    }
}
