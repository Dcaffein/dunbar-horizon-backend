package com.example.DunbarHorizon.buzz.application.service;

import com.example.DunbarHorizon.buzz.application.port.out.RecipientStrategyPort;
import com.example.DunbarHorizon.buzz.application.dto.info.RecipientType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RecipientStrategyProvider {
    private final Map<RecipientType, RecipientStrategyPort> strategies;

    public RecipientStrategyProvider(List<RecipientStrategyPort> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(RecipientStrategyPort::getSupportType, s -> s));
    }

    public RecipientStrategyPort getStrategy(RecipientType type) {
        return Optional.ofNullable(strategies.get(type))
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 타입: " + type));
    }
}
