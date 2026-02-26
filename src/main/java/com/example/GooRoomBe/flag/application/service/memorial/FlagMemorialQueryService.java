package com.example.GooRoomBe.flag.application.service.memorial;

import com.example.GooRoomBe.flag.application.port.in.FlagMemorialQueryUseCase;
import com.example.GooRoomBe.flag.application.port.in.dto.MemorialResponse;
import com.example.GooRoomBe.flag.application.port.out.FlagUserInfo;
import com.example.GooRoomBe.flag.application.port.out.FlagUserPort;
import com.example.GooRoomBe.flag.domain.memorial.FlagMemorial;
import com.example.GooRoomBe.flag.domain.memorial.repository.FlagMemorialRepository;
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
    public List<MemorialResponse> getMemorials(Long flagId, Long viewerId) {

        List<FlagMemorial> memorials = memorialRepository.findAllMemorialsIfMemorialized(flagId, viewerId);
        if (memorials.isEmpty()) return List.of();

        List<Long> writerIds = memorials.stream()
                .map(FlagMemorial::getWriterId)
                .distinct()
                .toList();

        Map<Long, FlagUserInfo> writerMap = flagUserPort.findUserInfosByIds(writerIds);

        return memorials.stream()
                .map(m -> MemorialResponse.of(
                        m,
                        writerMap.getOrDefault(m.getWriterId(),
                                new FlagUserInfo(m.getWriterId(), "알 수 없는 사용자", null))
                ))
                .toList();
    }
}
