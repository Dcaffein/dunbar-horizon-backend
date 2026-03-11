package com.example.DunbarHorizon.social.application;

import com.example.DunbarHorizon.social.application.service.LabelService;
import com.example.DunbarHorizon.social.domain.socialUser.SocialUser;
import com.example.DunbarHorizon.social.domain.socialUser.repository.SocialUserRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.LabelCreator;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import com.example.DunbarHorizon.social.domain.label.LabelMemberRegistry;
import com.example.DunbarHorizon.social.domain.label.LabelNamePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelServiceTest {

    @InjectMocks
    private LabelService labelService;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private SocialUserRepository socialUserRepository; // useCase 대신 Repository 주입 🫡

    @Mock
    private LabelNamePolicy labelNamePolicy;

    @Mock
    private LabelMemberRegistry labelMemberRegistry;

    @Mock
    private LabelCreator labelCreator;

    private final Long currentUserId = 1L;
    private final String labelId = "label-uuid-123";
    private Label mockLabel;

    @BeforeEach
    void setUp() {
        mockLabel = mock(Label.class);
    }

    @Test
    @DisplayName("라벨 생성 시 이름 중복 확인과 소유자 조회를 거쳐 저장한다")
    void createLabel_Success() {
        // given
        String name = "친구들";
        SocialUser mockOwner = mock(SocialUser.class); // Optional 타입을 위해 SocialUser로 mock 🛡️

        // given(...) 구문 수정: socialUserRepository 사용 및 Optional.of() 타입 일치
        given(socialUserRepository.findById(currentUserId)).willReturn(Optional.of(mockOwner));
        given(labelCreator.create(any(), anyString(), anyBoolean())).willReturn(mock(Label.class));
        given(labelRepository.save(any(Label.class))).willReturn(mockLabel);

        // when
        Label result = labelService.createLabel(currentUserId, name, true);

        // then
        verify(labelNamePolicy).validateLabelNameUniqueness(currentUserId, name);
        verify(socialUserRepository).findById(currentUserId);
        verify(labelRepository).save(any(Label.class));
        assertThat(result).isEqualTo(mockLabel);
    }

    @Test
    @DisplayName("라벨 삭제 시 소유자 확인 후 삭제를 수행한다")
    void deleteLabel_Success() {
        // given
        SocialUser owner = mock(SocialUser.class);
        given(owner.getId()).willReturn(currentUserId);

        given(labelRepository.findById(labelId)).willReturn(Optional.of(mockLabel));
        given(mockLabel.getOwner()).willReturn(owner);

        // when
        labelService.deleteLabel(currentUserId, labelId);

        // then
        verify(labelRepository).delete(mockLabel);
    }

    @Test
    @DisplayName("라벨에 멤버 추가 시 유저를 찾고 도메인 서비스를 호출한 뒤 저장한다")
    void addMemberToLabel_Success() {
        // given
        Long newMemberId = 2L;
        SocialUser newMember = mock(SocialUser.class);
        SocialUser owner = mock(SocialUser.class);
        given(owner.getId()).willReturn(currentUserId);

        given(labelRepository.findById(labelId)).willReturn(Optional.of(mockLabel));
        given(mockLabel.getOwner()).willReturn(owner);
        given(socialUserRepository.findById(newMemberId)).willReturn(Optional.of(newMember));

        // when
        // 🌟 수정된 부분: 인자 개수 3개로 맞춤 (currentUserId 추가)
        labelService.addMemberToLabel(currentUserId, labelId, newMemberId);

        // then
        verify(labelMemberRegistry).addNewMember(mockLabel, newMember);
        verify(labelRepository).save(mockLabel);
    }

    @Test
    @DisplayName("라벨 정보 업데이트 시 전달된 값이 있는 필드만 반영한다")
    void updateLabel_PartialUpdate() {
        // given
        String newName = "새이름";
        Boolean newExposure = null;

        given(labelRepository.findById(labelId)).willReturn(Optional.of(mockLabel));

        // when
        labelService.updateLabel(labelId, currentUserId, newName, newExposure);

        // then
        verify(labelNamePolicy).changeLabelName(mockLabel, newName);
        verify(mockLabel, never()).updateExposure(anyLong(), anyBoolean());
        verify(labelRepository).save(mockLabel);
    }
}