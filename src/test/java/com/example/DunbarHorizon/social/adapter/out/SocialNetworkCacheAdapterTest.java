package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialNetworkCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private SocialNetworkCacheAdapter cacheAdapter;

    @Test
    @DisplayName("evictDefaultNetwork: DunbarCircle 4종에 해당하는 모든 캐시 키를 삭제한다")
    void evictDefaultNetwork_DunbarCircle_4개_키를_모두_삭제한다() {
        // given
        Long userId = 1L;

        // when
        cacheAdapter.evictDefaultNetwork(userId);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, times(DunbarCircle.values().length)).delete(captor.capture());

        assertThat(captor.getAllValues()).containsExactlyInAnyOrder(
                "dunbar:network:default:1:SUPPORT",
                "dunbar:network:default:1:SYMPATHY",
                "dunbar:network:default:1:KINSHIP",
                "dunbar:network:default:1:DUNBAR"
        );
    }

    @Test
    @DisplayName("evictDefaultNetwork: userId가 달라도 각 userId에 맞는 키를 삭제한다")
    void evictDefaultNetwork_userId에_맞는_키를_삭제한다() {
        // given
        Long userId = 99L;

        // when
        cacheAdapter.evictDefaultNetwork(userId);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, times(4)).delete(captor.capture());
        assertThat(captor.getAllValues()).allMatch(key -> key.startsWith("dunbar:network:default:99:"));
    }

    @Test
    @DisplayName("evictLabelNetwork: 정확한 라벨 캐시 키를 삭제한다")
    void evictLabelNetwork_정확한_키를_삭제한다() {
        // given
        Long userId = 1L;
        String labelId = "label-uuid-123";

        // when
        cacheAdapter.evictLabelNetwork(userId, labelId);

        // then
        verify(redisTemplate).delete("dunbar:network:label:1:label-uuid-123");
        verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    @DisplayName("evictAllLabelNetworks: 패턴에 매칭되는 모든 라벨 키를 한 번에 삭제한다")
    void evictAllLabelNetworks_패턴_매칭_키를_모두_삭제한다() {
        // given
        Long userId = 2L;
        Set<String> matchedKeys = Set.of(
                "dunbar:network:label:2:label-A",
                "dunbar:network:label:2:label-B",
                "dunbar:network:label:2:label-C"
        );
        given(redisTemplate.keys("dunbar:network:label:2:*")).willReturn(matchedKeys);

        // when
        cacheAdapter.evictAllLabelNetworks(userId);

        // then
        verify(redisTemplate).delete(matchedKeys);
    }

    @Test
    @DisplayName("evictAllLabelNetworks: keys()가 null을 반환하면 delete를 호출하지 않는다")
    void evictAllLabelNetworks_keys_null이면_삭제_호출_없음() {
        // given
        given(redisTemplate.keys(anyString())).willReturn(null);

        // when
        cacheAdapter.evictAllLabelNetworks(99L);

        // then — Redis cluster 환경(keys 미지원)에서 NPE 없이 안전하게 종료되어야 한다
        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    @DisplayName("evictAllLabelNetworks: keys()가 빈 Set을 반환하면 delete를 호출하지 않는다")
    void evictAllLabelNetworks_keys_비어있으면_삭제_호출_없음() {
        // given
        given(redisTemplate.keys(anyString())).willReturn(Set.of());

        // when
        cacheAdapter.evictAllLabelNetworks(99L);

        // then
        verify(redisTemplate, never()).delete(anyCollection());
    }
}
