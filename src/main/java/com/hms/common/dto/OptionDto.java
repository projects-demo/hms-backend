package com.hms.common.dto;

/**
 * Minimal id+label(+subLabel) projection for typeahead / autocomplete widgets.
 * Every autocomplete endpoint in the system returns List<OptionDto> instead of
 * full entities - keeps payloads tiny and avoids leaking full records into
 * dropdown searches.
 */
public record OptionDto(Long id, String label, String subLabel) {
    public OptionDto(Long id, String label) {
        this(id, label, null);
    }
}
