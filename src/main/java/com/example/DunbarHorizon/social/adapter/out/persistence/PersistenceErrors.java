package com.example.DunbarHorizon.social.adapter.out.persistence;

import java.sql.SQLException;

final class PersistenceErrors {

    private static final int MYSQL_DUPLICATE_KEY = 1062;

    private PersistenceErrors() {
    }

    static boolean isDuplicateKey(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getErrorCode() == MYSQL_DUPLICATE_KEY) {
                return true;
            }
        }
        return false;
    }
}
