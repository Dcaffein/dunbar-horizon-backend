package com.example.DunbarHorizon.buzz.application.port.out;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BuzzPurgeRepository {
    List<Long> findPurgeableIds(LocalDateTime createdAtOrBefore);
    void purge(Collection<Long> buzzIds);
}
