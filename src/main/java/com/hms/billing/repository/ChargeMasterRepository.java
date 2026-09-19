package com.hms.billing.repository;

import com.hms.billing.entity.ChargeMaster;
import com.hms.common.dto.OptionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChargeMasterRepository extends JpaRepository<ChargeMaster, Long> {

    Optional<ChargeMaster> findByCode(String code);

    @Query("""
           select c from ChargeMaster c join fetch c.chargeType
           where c.active = true and (:q is null or lower(c.name) like lower(concat('%', :q, '%')))
           """)
    Page<ChargeMaster> search(@Param("q") String q, Pageable pageable);

    @Query("""
           select new com.hms.common.dto.OptionDto(c.id, c.name, c.code)
           from ChargeMaster c where c.active = true and lower(c.name) like lower(concat(:q, '%'))
           """)
    List<OptionDto> autocomplete(@Param("q") String q, Pageable pageable);
}
