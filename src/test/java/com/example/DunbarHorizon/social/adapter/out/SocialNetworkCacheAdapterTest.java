package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.redis.SocialNetworkCacheAdapter;
import com.example.DunbarHorizon.social.domain.friend.DunbarCircle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
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
}
