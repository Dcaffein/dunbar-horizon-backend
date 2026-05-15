package com.example.DunbarHorizon.social.adapter.out.neo4j;

import com.example.DunbarHorizon.social.adapter.out.neo4j.springData.FriendRequestNeo4jRepository;
import com.example.DunbarHorizon.social.adapter.out.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.application.service.FriendRequestRequesterActionService;
import com.example.DunbarHorizon.social.domain.friend.exception.DuplicateFriendRequestException;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.support.TestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class FriendRequestConcurrencyTest {

    @Autowired private FriendRequestRequesterActionService friendRequestRequesterActionService;
    @Autowired private FriendRequestNeo4jRepository friendRequestNeo4jRepository;
    @Autowired private SocialUserNeo4jRepository socialUserNeo4jRepository;

    private SocialUser userA;
    private SocialUser userB;

    @BeforeEach
    void setUp() {
        userA = socialUserNeo4jRepository.save(new SocialUser(901L, "concurrencyUserA", ""));
        userB = socialUserNeo4jRepository.save(new SocialUser(902L, "concurrencyUserB", ""));
    }

    @AfterEach
    void tearDown() {
        friendRequestNeo4jRepository.deleteAll();
        socialUserNeo4jRepository.deleteById(userA.getId());
        socialUserNeo4jRepository.deleteById(userB.getId());
    }

    @Test
    @DisplayName("A→B와 B→A 요청이 동시에 들어오면 pairKey 제약으로 하나만 성공하고 나머지는 DuplicateFriendRequestException이 발생한다")
    void concurrentReverseDirectionRequests_OnlyOneSucceeds() throws InterruptedException {
        // given
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Runnable taskAtoB = () -> {
            try {
                startLatch.await();
                friendRequestRequesterActionService.sendRequest(userA.getId(), userB.getId());
                successCount.incrementAndGet();
            } catch (DuplicateFriendRequestException e) {
                failCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable taskBtoA = () -> {
            try {
                startLatch.await();
                friendRequestRequesterActionService.sendRequest(userB.getId(), userA.getId());
                successCount.incrementAndGet();
            } catch (DuplicateFriendRequestException e) {
                failCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(taskAtoB);
        executor.submit(taskBtoA);
        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(completed).as("두 스레드가 5초 내에 완료되어야 한다").isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(friendRequestNeo4jRepository.count()).isEqualTo(1);
    }
}
