package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.application.dto.result.MutualFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkFriendEdgeResult;
import com.example.DunbarHorizon.social.application.dto.result.NetworkOneHopsByTwoHopResult;
import com.example.DunbarHorizon.social.application.port.in.SocialNetworkQueryUseCase;
import com.example.DunbarHorizon.social.application.port.out.SocialNetworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialNetworkQueryService implements SocialNetworkQueryUseCase {

    private final SocialNetworkRepository socialNetworkRepository;

    /**
     * 메인 홈 네트워크: 친밀도 기반 동적 프루닝이 적용된 글로벌 네트워크 (15, 50, 150명)
     */
    @Override
    public List<NetworkFriendEdgeResult> getFriendsNetwork(Long userId, int limitSize) {
        return socialNetworkRepository.getDefaultIntimacyNetwork(userId, limitSize);
    }

    /**
     * 라벨 네트워크: 특정 라벨(그룹) 내부의 동적 프루닝 네트워크
     */
    @Override
    public List<NetworkFriendEdgeResult> getLabelNetwork(Long userId, String labelName, int limitSize) {
        return socialNetworkRepository.getLabelCustomNetwork(userId, labelName, limitSize);
    }

    /**
     * 1-Hop 친구 수동 추가 (Drag & Drop):
     * 클라이언트가 전달한 컨텍스트(labelName, limitSize)를 바탕으로 백엔드에서 뼈대를 재구성하여 교집합 엣지만 반환
     */
    @Override
    public List<MutualFriendEdgeResult> getIntersectionByOneHop(
            Long userId, Long targetId, String labelName, int limitSize) {
        return socialNetworkRepository.getIntersectionByOneHop(userId, targetId, labelName, limitSize);
    }

    /**
     * 2-Hop 유저 추천:
     * 클라이언트가 전달한 컨텍스트(labelName, limitSize) 바운더리 내에서, 이방인 페널티를 적용하여 조회
     */
    @Override
    public List<NetworkOneHopsByTwoHopResult> getIntersectionByTwoHop(
            Long userId, Long targetId, String labelName, int limitSize) {
        return socialNetworkRepository.getIntersectionOneHops(userId, targetId, labelName, limitSize);
    }
}