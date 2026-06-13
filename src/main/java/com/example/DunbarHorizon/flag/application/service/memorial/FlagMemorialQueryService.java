package com.example.DunbarHorizon.flag.application.service.memorial;

import com.example.DunbarHorizon.flag.application.port.in.FlagMemorialQueryUseCase;
import com.example.DunbarHorizon.flag.application.dto.info.FlagUserInfo;
import com.example.DunbarHorizon.flag.application.dto.result.MemorialListResult;
import com.example.DunbarHorizon.flag.application.dto.result.MemorialResult;
import com.example.DunbarHorizon.flag.application.port.out.FlagUserPort;
import com.example.DunbarHorizon.flag.domain.memorial.FlagMemorial;
import com.example.DunbarHorizon.flag.domain.memorial.repository.FlagMemorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlagMemorialQueryService implements FlagMemorialQueryUseCase {

    private final FlagMemorialRepository memorialRepository;
    private final FlagUserPort flagUserPort;

    @Override
    public MemorialListResult getMemorials(Long flagId, Long viewerId) {
        if (!memorialRepository.existsByFlagId(flagId)) {
            return MemorialListResult.empty();
        }
        if (!memorialRepository.existsByFlagIdAndWriterId(flagId, viewerId)) {
            return MemorialListResult.asLocked();
        }

        List<FlagMemorial> memorials = memorialRepository.findAllByFlagId(flagId);
        List<Long> writerIds = memorials.stream()
                .map(FlagMemorial::getWriterId)
                .distinct()
                .toList();

        Map<Long, FlagUserInfo> writerMap = flagUserPort.findUserInfosByIds(writerIds);

        return MemorialListResult.of(
                memorials.stream()
                        .map(m -> MemorialResult.of(
                                m,
                                writerMap.getOrDefault(m.getWriterId(),
                                        new FlagUserInfo(m.getWriterId(), "알 수 없는 사용자", null))))
                        .toList()
        );
    }
}
