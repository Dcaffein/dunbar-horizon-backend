package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.LabelRepositoryAdapter;
import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.SocialUserNeo4jRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelTestFactory;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.support.Neo4jRepositoryTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 커밋이 필요하므로 테스트 트랜잭션을 끄고, 각 쓰기 커맨드가 자기 트랜잭션에서 커밋되게 한다.
 */
@Neo4jRepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(LabelRepositoryAdapter.class)
class LabelConcurrentWriteTest {

    @Autowired
    private LabelRepositoryAdapter labelRepository;

    @Autowired
    private SocialUserNeo4jRepository socialUserNeo4jRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    private Label label;

    @BeforeEach
    void setUp() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        SocialUser owner = socialUserNeo4jRepository.save(new SocialUser(1L, "소유자", null));
        LongStream.rangeClosed(2, 9).forEach(id ->
                socialUserNeo4jRepository.save(new SocialUser(id, "친구" + id, null)));
        label = LabelTestFactory.createLabel(owner, "운동 모임");
        labelRepository.create(label);
    }

    @AfterEach
    void tearDown() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }

    @Test
    @DisplayName("같은 라벨에 서로 다른 멤버를 동시에 추가하면 예외 없이 모두 반영된다")
    void addMember_동시_추가가_모두_반영된다() throws Exception {
        // given
        List<Long> memberIds = LongStream.rangeClosed(2, 9).boxed().toList();
        List<Callable<Void>> tasks = memberIds.stream()
                .<Callable<Void>>map(memberId -> () -> {
                    labelRepository.addMember(label.getId(), memberId);
                    return null;
                })
                .toList();

        // when
        runConcurrently(tasks);

        // then
        assertThat(memberIdsOf(label.getId())).containsExactlyInAnyOrderElementsOf(memberIds);
    }

    @Test
    @DisplayName("같은 라벨에 서로 다른 목록으로 동시에 교체하면 두 목록의 합집합이 아니라 한쪽 목록만 남는다")
    void replaceMembers_동시_교체는_한쪽_목록만_남긴다() throws Exception {
        Set<Long> first = Set.of(3L, 4L, 5L);
        Set<Long> second = Set.of(6L, 7L, 8L);

        for (int round = 0; round < 20; round++) {
            // given
            labelRepository.replaceMembers(label.getId(), List.of(2L, 9L));

            // when
            runConcurrently(List.of(
                    () -> { labelRepository.replaceMembers(label.getId(), first); return null; },
                    () -> { labelRepository.replaceMembers(label.getId(), second); return null; }
            ));

            // then
            assertThat(memberIdsOf(label.getId())).isIn(first, second);
        }
    }

    private void runConcurrently(List<Callable<Void>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        try {
            for (Callable<Void> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            ready.await();
            start.countDown();
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private Set<Long> memberIdsOf(String labelId) {
        return new HashSet<>(neo4jClient.query("MATCH (:Label {id: $labelId})-[:ATTACHED_TO]->(m) RETURN m.id")
                .bind(labelId).to("labelId")
                .fetchAs(Long.class)
                .all());
    }
}
