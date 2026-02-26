package com.example.GooRoomBe.social.domain.label;

import com.example.GooRoomBe.social.domain.label.repository.LabelRepository;
import com.example.GooRoomBe.social.domain.socialUser.SocialUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LabelNamePolicyTest {

    @InjectMocks
    private LabelNamePolicy labelNamePolicy;
    @Mock
    private LabelRepository labelRepository;

    @Test
    @DisplayName("기존 이름과 동일한 이름으로 변경 시 중복 검사를 하지 않고 종료한다")
    void changeLabelName_SameName_Skip() {
        // given
        SocialUser owner = new SocialUser(1L, "owner", null);
        Label label = new Label(owner, "기존이름", true);

        // when
        labelNamePolicy.changeLabelName(label, "기존이름");

        // then
        verify(labelRepository, never()).existsByOwner_IdAndLabelName(any(), any());
    }

    @Test
    @DisplayName("다른 유저가 가진 라벨 이름이라도 내 라벨 이름과 중복되지 않으면 변경 가능하다")
    void changeLabelName_Success() {
        // given
        SocialUser owner = new SocialUser(1L, "owner", null);
        Label label = new Label(owner, "기존이름", true);
        String newName = "새로운이름";

        given(labelRepository.existsByOwner_IdAndLabelName(owner.getId(), newName))
                .willReturn(false);

        // when
        labelNamePolicy.changeLabelName(label, newName);

        // then
        assertThat(label.getLabelName()).isEqualTo(newName);
    }
}