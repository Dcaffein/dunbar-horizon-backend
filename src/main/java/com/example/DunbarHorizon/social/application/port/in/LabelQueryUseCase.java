package com.example.DunbarHorizon.social.application.port.in;

import java.util.List;
import java.util.Set;

public interface LabelQueryUseCase {
    Set<Long> getMemberIdsInOwnedLabels(Long ownerId, List<String> labelIds);
}
