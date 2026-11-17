package com.example.DunbarHorizon.social.adapter.out.persistence;

import org.hibernate.exception.ConstraintViolationException;

final class PersistenceErrors {

    private PersistenceErrors() {
    }

    static boolean violates(Throwable e, String constraintName) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                String violated = violation.getConstraintName();
                return violated.substring(violated.lastIndexOf('.') + 1).equals(constraintName);
            }
        }
        return false;
    }
}
