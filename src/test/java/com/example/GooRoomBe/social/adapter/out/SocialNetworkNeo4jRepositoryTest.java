package com.example.GooRoomBe.social.adapter.out;

import com.example.GooRoomBe.social.adapter.out.neo4j.dsl.SocialNetworkNeo4jRepository;
import com.example.GooRoomBe.social.application.dto.NetworkBetweenOneHopsResponse;
import com.example.GooRoomBe.social.application.dto.NetworkTwoHopSuggestionsResponse;
import com.example.GooRoomBe.social.application.dto.NetworkOneHopsByTwoHopResponse;
import com.example.GooRoomBe.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Neo4jRepositoryTest
@Import(SocialNetworkNeo4jRepository.class)
class SocialNetworkNeo4jRepositoryTest {

    @Autowired
    private SocialNetworkNeo4jRepository socialNetworkRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeEach
    void setupGraph() {
        // 기존 데이터 초기화
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();

        neo4jClient.query("""
        CREATE (me:UserReference {id: 1, nickname: '나'})
        CREATE (fa:UserReference {id: 2, nickname: '친구A'})
        CREATE (fb:UserReference {id: 3, nickname: '친구B'})
        CREATE (mf:UserReference {id: 4, nickname: '공통친구'})
        CREATE (t2:UserReference {id: 5, nickname: '추천대상'})
        CREATE (lonely:UserReference {id: 99, nickname: '친구없는유저'})

        // 1-Hop: 나와 친구A (intimacy: 0.8, interestScore: 50.0)
        CREATE (fs1:FriendShip {intimacy: 0.8})
        CREATE (me)-[:MEMBER_OF {interestScore: 50.0, friendAlias: '베프', isRoutable: true}]->(fs1)
        CREATE (fa)-[:MEMBER_OF {interestScore: 50.0}]->(fs1)

        // 1-Hop: 나와 친구B (intimacy: 0.5)
        CREATE (fs2:FriendShip {intimacy: 0.5})
        CREATE (me)-[:MEMBER_OF {interestScore: 10.0, friendAlias: '동료', isRoutable: true}]->(fs2)
        CREATE (fb)-[:MEMBER_OF]->(fs2)

        // 1-Hop: 나와 공통친구
        CREATE (fs3:FriendShip {intimacy: 0.3})
        CREATE (me)-[:MEMBER_OF {interestScore: 5.0, isRoutable: true}]->(fs3)
        CREATE (mf)-[:MEMBER_OF]->(fs3)

        // 친구A와 공통친구 사이의 상호 관계 및 친밀도 (Shared Intimacy: 0.9)
        CREATE (fs4:FriendShip {intimacy: 0.9})
        CREATE (fa)-[:MEMBER_OF]->(fs4)
        CREATE (mf)-[:MEMBER_OF]->(fs4)

        // 친구A와 추천대상(5) 사이의 관계
        CREATE (fs5:FriendShip {intimacy: 0.6})
        CREATE (fa)-[:MEMBER_OF]->(fs5)
        CREATE (t2)-[:MEMBER_OF]->(fs5)

        // 2-Hop 추천을 위한 공통 라벨 설정
        CREATE (lab:Label {labelName: '취미공유', exposure: true})
        CREATE (fa)-[:OWNS]->(lab)
        CREATE (lab)-[:HAS_MEMBER]->(me)
        CREATE (lab)-[:HAS_MEMBER]->(t2)
        """).run();
    }

    @Test
    @DisplayName("1-Hop 네트워크 조회 시 본인의 관심도와 상호 친밀도, 상호 친구간 친밀도를 모두 확인한다")
    void getOneHopsNetwork_Success() {
        List<NetworkBetweenOneHopsResponse> result = socialNetworkRepository.getOneHopsNetwork(1L);

        assertThat(result).hasSize(3);

        // 친구A(2L)에 대한 응답 검증
        NetworkBetweenOneHopsResponse dtoA = result.stream()
                .filter(d -> d.friendId().equals(2L)).findFirst().orElseThrow();

        // 1. 관심도 및 친밀도 확인
        assertThat(dtoA.myInterestScore()).isEqualTo(50.0);
        assertThat(dtoA.meAndFriendIntimacy()).isEqualTo(0.8);

        // 2. 도메인 정책에 따른 정규화 점수 확인 (50 / (50 + 50) = 0.5)
        assertThat(dtoA.getNormalizedMyInterest()).isCloseTo(0.5, within(0.01));

        // 3. 상호 친구 정보 및 그들 사이의 친밀도 확인
        assertThat(dtoA.mutualFriends()).hasSize(1);
        assertThat(dtoA.mutualFriends().get(0).mutualFriendId()).isEqualTo(4L);
        assertThat(dtoA.mutualFriends().get(0).mutualIntimacy()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("특정 피벗을 기준으로 피벗 본인을 제외한 연관 유저를 추천한다")
    void getTwoHopSuggestions_ByPivot_Success() {
        Long pivotId = 2L; // 친구A를 피벗으로 설정
        List<NetworkTwoHopSuggestionsResponse> result = socialNetworkRepository.getTwoHopSuggestionsByOneHop(1L, pivotId);

        assertThat(result).isNotEmpty();
        // 추천 대상(5L)이 포함되어야 하며, 피벗 본인(2L)은 추천 목록에 없어야 함
        assertThat(result).extracting(NetworkTwoHopSuggestionsResponse::suggestedFriendId)
                .contains(5L)
                .doesNotContain(2L);
    }

    @Test
    @DisplayName("특정 타겟 유저와 공유하는 1-Hop 인맥(Intersection)을 조회한다")
    void getIntersectionOneHops_Success() {
        Long targetId = 5L; // 추천대상

        List<NetworkOneHopsByTwoHopResponse> result = socialNetworkRepository.getIntersectionOneHops(1L, targetId);

        // 나와 추천대상을 동시에 알고 있는 친구는 친구A(2L) 뿐임
        assertThat(result).hasSize(1);
        assertThat(result.get(0).friendId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("친구가 없는 유저가 조회할 경우 모든 결과는 빈 리스트여야 한다")
    void getNetwork_Empty_WhenNoFriends() {
        Long lonelyUserId = 99L;

        // 1. 1-Hop 네트워크 조회
        List<NetworkBetweenOneHopsResponse> oneHops = socialNetworkRepository.getOneHopsNetwork(lonelyUserId);
        assertThat(oneHops).isEmpty();

        // 2. 피벗 기반 추천 조회 (피벗을 2L로 가정해도 결과는 없어야 함)
        List<NetworkTwoHopSuggestionsResponse> suggestions = socialNetworkRepository.getTwoHopSuggestionsByOneHop(lonelyUserId, 2L);
        assertThat(suggestions).isEmpty();

        // 3. 교집합 인맥 조회
        List<NetworkOneHopsByTwoHopResponse> intersections = socialNetworkRepository.getIntersectionOneHops(lonelyUserId, 5L);
        assertThat(intersections).isEmpty();
    }
}