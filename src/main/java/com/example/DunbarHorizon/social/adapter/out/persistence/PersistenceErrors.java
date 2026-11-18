package com.example.DunbarHorizon.social.adapter.out.persistence;

import org.hibernate.exception.ConstraintViolationException;

final class PersistenceErrors {

    private PersistenceErrors() {
    }

    static boolean violates(Throwable e, String constraintName) {
        String violated = violatedConstraint(e);
        return violated != null && violated.substring(violated.lastIndexOf('.') + 1).equals(constraintName);
    }

    static boolean violatesPrimaryKey(Throwable e, String tableName) {
        return (tableName + ".PRIMARY").equals(violatedConstraint(e));
    }

    private static String violatedConstraint(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
