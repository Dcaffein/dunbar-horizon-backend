package com.example.GooRoomBe.flag.application.service.flag;

import com.example.GooRoomBe.flag.application.port.in.FlagQueryUseCase;
import com.example.GooRoomBe.flag.application.port.in.FlagRole;
import com.example.GooRoomBe.flag.application.port.in.dto.FlagDetailResponse;
import com.example.GooRoomBe.flag.application.port.in.dto.FlagResponse;
import com.example.GooRoomBe.flag.application.port.in.dto.ParticipantResponse;
import com.example.GooRoomBe.flag.application.port.out.FlagUserInfo;
import com.example.GooRoomBe.flag.application.port.out.FlagUserPort;
import com.example.GooRoomBe.flag.domain.flag.Flag;
import com.example.GooRoomBe.flag.domain.flag.FlagParticipant;
import com.example.GooRoomBe.flag.domain.flag.FlagStatus;
import com.example.GooRoomBe.flag.domain.flag.exception.FlagNotFoundException;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagParticipantRepository;
import com.example.GooRoomBe.flag.domain.flag.repository.FlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlagQueryService implements FlagQueryUseCase {

    private final FlagRepository flagRepository;
    private final FlagParticipantRepository participantRepository;
    private final FlagUserPort flagUserPort;

    @Override
    public List<FlagResponse> getFriendFlags(Long userId) {
        Set<Long> friendIds = flagUserPort.getRelatedUserIds(userId);
        if (friendIds.isEmpty()) return List.of();

        List<Flag> recruitingFlags = flagRepository.findAllByHostIdsAndStatus(friendIds, FlagStatus.RECRUITING);

        Map<Long, FlagUserInfo> hostInfoMap = flagUserPort.findUserInfosByIds(friendIds);

        return recruitingFlags.stream()
                .map(flag -> FlagResponse.of(flag, hostInfoMap.getOrDefault(flag.getHostId(), null)))
                .toList();
    }


    @Override
    public List<FlagResponse> getMyFlagsByRole(Long userId, FlagRole role) {
        return switch (role) {
            case HOST -> getMyManagedFlags(userId);
            case PARTICIPANT -> getParticipatingFlags(userId);
            case GUEST -> List.of();
        };
    }

    @Override
    public FlagDetailResponse getFlagDetail(Long flagId, Long viewerId) {
        Flag flag = flagRepository.findById(flagId)
                .orElseThrow(() -> new FlagNotFoundException(flagId));

        List<FlagParticipant> participants = participantRepository.findAllByFlagId(flagId);

        boolean isHost = flag.getHostId().equals(viewerId);
        boolean isParticipant = participants.stream().anyMatch(p -> p.getParticipantId().equals(viewerId));

        Set<Long> allUserIds = participants.stream()
                .map(FlagParticipant::getParticipantId)
                .collect(Collectors.toCollection(HashSet::new));
        allUserIds.add(flag.getHostId());

        Map<Long, FlagUserInfo> userInfoMap = flagUserPort.findUserInfosByIds(allUserIds);

        List<ParticipantResponse> participantResponses = participants.stream()
                .map(p -> ParticipantResponse.of(userInfoMap.get(p.getParticipantId()), p.getCreatedAt()))
                .toList();

        FlagUserInfo hostInfo = userInfoMap.get(flag.getHostId());

        FlagRole role = determineViewerRole(isHost, isParticipant);

        return FlagDetailResponse.of(flag, hostInfo, participantResponses, role);
    }

    private List<FlagResponse> getMyManagedFlags(Long userId) {
        List<Flag> managedFlags = flagRepository.findAllByHostId(userId);
        FlagUserInfo myInfo = flagUserPort.findUserInfosByIds(Set.of(userId)).get(userId);

        return managedFlags.stream()
                .map(flag -> FlagResponse.of(flag, myInfo))
                .toList();
    }

    private List<FlagResponse> getParticipatingFlags(Long userId) {
        List<Long> flagIds = participantRepository.findFlagIdByParticipantId(userId);
        if (flagIds.isEmpty()) return List.of();

        List<Flag> flags = flagRepository.findAllByIdIn(flagIds);
        Set<Long> hostIds = flags.stream().map(Flag::getHostId).collect(Collectors.toSet());
        Map<Long, FlagUserInfo> hostInfoMap = flagUserPort.findUserInfosByIds(hostIds);

        return flags.stream()
                .map(flag -> FlagResponse.of(flag, hostInfoMap.get(flag.getHostId())))
                .toList();
    }

    private FlagRole determineViewerRole(boolean isHost, boolean isParticipant) {
        if (isHost) return FlagRole.HOST;
        if (isParticipant) return FlagRole.PARTICIPANT;
        return FlagRole.GUEST;
    }
}