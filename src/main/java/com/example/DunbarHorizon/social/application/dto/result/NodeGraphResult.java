package com.example.DunbarHorizon.social.application.dto.result;

import java.util.List;

public record NodeGraphResult(Long nodeId, double interestScore, List<NodeEdgeResult> edges) {}
