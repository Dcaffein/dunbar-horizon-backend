package com.example.DunbarHorizon.social.application.network.dto;

import java.util.List;

public record NodeGraphResult(Long nodeId, double interestScore, List<NodeEdgeResult> edges) {}
