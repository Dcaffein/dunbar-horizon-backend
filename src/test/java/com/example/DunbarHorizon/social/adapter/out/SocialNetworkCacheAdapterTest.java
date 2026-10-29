package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.redis.SocialNetworkCacheAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialNetworkCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private SocialNetworkCacheAdapter cacheAdapter;

    @Test
    @DisplayName("evictDefaultNetwork: DUNBAR 서클 캐시 키 하나만 삭제한다")
    void evictDefaultNetwork_DUNBAR_키만_삭제한다() {
        // given
        Long userId = 1L;

        // when
        cacheAdapter.evictDefaultNetwork(userId);

        // then
        verify(redisTemplate).delete("dunbar:network:default:1:DUNBAR");
        verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    @DisplayName("evictDefaultNetwork: userId에 맞는 키를 삭제한다")
    void evictDefaultNetwork_userId에_맞는_키를_삭제한다() {
        // given
        Long userId = 99L;

        // when
        cacheAdapter.evictDefaultNetwork(userId);

        // then
        verify(redisTemplate).delete("dunbar:network:default:99:DUNBAR");
        verifyNoMoreInteractions(redisTemplate);
    }
}
