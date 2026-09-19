package com.hms.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Every controller builds its Pageable through here instead of trusting
 * client-supplied `size` directly - caps it at hms.pagination.max-page-size
 * so nobody can request size=999999 and defeat the point of pagination.
 */
@Component
public class PageRequestFactory {

    @Value("${hms.pagination.default-page-size:20}")
    private int defaultSize;

    @Value("${hms.pagination.max-page-size:100}")
    private int maxSize;

    public Pageable of(Integer page, Integer size, String sortField, String direction) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0) ? defaultSize : Math.min(size, maxSize);
        if (sortField == null || sortField.isBlank()) {
            return PageRequest.of(p, s);
        }
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(p, s, Sort.by(dir, sortField));
    }

    public Pageable of(Integer page, Integer size) {
        return of(page, size, null, null);
    }
}
