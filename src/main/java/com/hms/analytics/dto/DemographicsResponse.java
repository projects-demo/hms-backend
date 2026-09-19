package com.hms.analytics.dto;

import java.util.List;

public record DemographicsResponse(List<GenderSlice> genderBreakdown, List<AgeGroupSlice> ageGroupBreakdown) {
    public record GenderSlice(String gender, long count) {}
    public record AgeGroupSlice(String ageGroup, long count) {}
}