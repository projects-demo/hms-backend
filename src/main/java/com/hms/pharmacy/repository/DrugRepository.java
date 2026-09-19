package com.hms.pharmacy.repository;

import com.hms.common.dto.OptionDto;
import com.hms.pharmacy.entity.Drug;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DrugRepository extends JpaRepository<Drug, Long> {

    Optional<Drug> findByDrugCode(String drugCode);

    @Query("""
           select d from Drug d
           where d.active = true and (:q is null or lower(d.name) like lower(concat('%', :q, '%'))
                or lower(d.genericName) like lower(concat('%', :q, '%')))
           """)
    Page<Drug> search(@Param("q") String q, Pageable pageable);

    @Query("""
           select new com.hms.common.dto.OptionDto(d.id, d.name, d.genericName)
           from Drug d where d.active = true and lower(d.name) like lower(concat(:q, '%'))
           """)
    List<OptionDto> autocomplete(@Param("q") String q, Pageable pageable);
}
