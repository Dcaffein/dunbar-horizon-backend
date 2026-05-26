package com.example.DunbarHorizon.social.application.service;

import com.example.DunbarHorizon.social.adapter.out.persistence.jpa.ArchivedFriendship;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchiveCandidate;
import com.example.DunbarHorizon.social.domain.friend.FriendshipArchivePolicy;
import com.example.DunbarHorizon.social.domain.friend.repository.ArchivedFriendshipRepository;
import com.example.DunbarHorizon.social.domain.friend.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendshipArchiveServiceTest {

    @InjectMocks
    private FriendshipArchiveService archiveService;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private ArchivedFriendshipRepository archivedFriendshipRepository;

    @Mock
    private FriendshipArchivePolicy archivePolicy;

    private static final double THRESHOLD = 0.0196;

    @Test
    @DisplayName("후보가 없으면 저장하지 않고 빈 목록을 반환한다")
    void archiveFriendships_후보없으면_저장안함() {
        given(archivePolicy.archiveThreshold()).willReturn(THRESHOLD);
        given(friendshipRepository.findArchiveCandidates(THRESHOLD)).willReturn(List.of());

        List<String> result = archiveService.archiveFriendships();

        assertThat(result).isEmpty();
        verify(archivedFriendshipRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("후보가 있으면 MySQL에 저장하고 friendshipId 목록을 반환한다")
    void archiveFriendships_후보있으면_MySQL저장후_IDs반환() {
        FriendshipArchiveCandidate candidate1 = new FriendshipArchiveCandidate("1_2", 1L, 2L, LocalDate.of(2025, 1, 1));
        FriendshipArchiveCandidate candidate2 = new FriendshipArchiveCandidate("3_4", 3L, 4L, LocalDate.of(2025, 3, 1));

        given(archivePolicy.archiveThreshold()).willReturn(THRESHOLD);
        given(friendshipRepository.findArchiveCandidates(THRESHOLD)).willReturn(List.of(candidate1, candidate2));

        List<String> result = archiveService.archiveFriendships();

        assertThat(result).containsExactlyInAnyOrder("1_2", "3_4");

        ArgumentCaptor<List<ArchivedFriendship>> captor = ArgumentCaptor.forClass(List.class);
        verify(archivedFriendshipRepository).saveAll(captor.capture());

        List<ArchivedFriendship> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getId()).isEqualTo("1_2");
        assertThat(saved.get(1).getId()).isEqualTo("3_4");
    }

    @Test
    @DisplayName("deleteArchivedFromNeo4j는 전달받은 IDs를 friendshipRepository에 위임한다")
    void deleteArchivedFromNeo4j_IDs전달_검증() {
        List<String> ids = List.of("1_2", "3_4");

        archiveService.deleteArchivedFromNeo4j(ids);

        verify(friendshipRepository).deleteAllByIds(ids);
    }
}
