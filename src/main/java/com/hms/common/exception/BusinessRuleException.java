package com.hms.common.exception;

/** Thrown when a request is well-formed but violates a domain rule
 *  (e.g. "cannot discharge patient with unpaid bill", "slot already booked"). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
