package com.hms.billing.entity;

/** Who referred or advised the patient for this bill. */
public enum AdvisorType {
    /** An internal doctor in this hospital — OPD fee comes from the doctor's set consultationFee. */
    INTERNAL_DOCTOR,
    /** Patient is self-referred or walk-in with no specific advisor. */
    SELF,
    /** A doctor external to this hospital (name recorded in comments). */
    EXTERNAL_DOCTOR,
}

