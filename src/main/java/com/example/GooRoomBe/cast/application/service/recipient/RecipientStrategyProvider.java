package com.example.GooRoomBe.cast.application.service.recipient;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RecipientStrategyProvider {
    private final Map<RecipientType, RecipientStrategy> strategies;

    public RecipientStrategyProvider(List<RecipientStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(RecipientStrategy::getSupportType, s -> s));
    }

    public RecipientStrategy getStrategy(RecipientType type) {
        return Optional.ofNullable(strategies.get(type))
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 타입: " + type));
    }
}
