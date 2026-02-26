package com.example.GooRoomBe.social.adapter.in.web;

import com.example.GooRoomBe.social.adapter.in.dto.LabelCreateRequest;
import com.example.GooRoomBe.social.adapter.in.dto.LabelMemberAddRequest;
import com.example.GooRoomBe.social.adapter.in.dto.LabelMembersReplaceRequest;
import com.example.GooRoomBe.social.adapter.in.dto.LabelUpdateRequest;
import com.example.GooRoomBe.social.application.service.LabelService;
import com.example.GooRoomBe.social.adapter.in.LabelController;
import com.example.GooRoomBe.social.domain.label.Label;
import com.example.GooRoomBe.social.domain.socialUser.SocialUser;
import com.example.GooRoomBe.support.BaseControllerTest;
import com.example.GooRoomBe.support.WithMockCustomUser;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LabelController.class)
@WithMockCustomUser()
class LabelControllerTest extends BaseControllerTest {

    @MockitoBean
    private LabelService labelService;

    private final String labelId = "label-uuid-123";

    @Test
    @DisplayName("새로운 라벨을 성공적으로 생성한다")
    void createLabel_Success() throws Exception {
        // given
        LabelCreateRequest dto = new LabelCreateRequest("친한친구", true);

        Label mockLabel = mock(Label.class);
        SocialUser mockOwner = mock(SocialUser.class);

        // ID가 String을 반환하도록 설정
        given(mockLabel.getId()).willReturn(labelId);
        given(mockLabel.getLabelName()).willReturn("친한친구");
        given(mockLabel.isExposure()).willReturn(true);
        given(mockLabel.getOwner()).willReturn(mockOwner);
        given(mockOwner.getId()).willReturn(1L);

        given(labelService.createLabel(eq(1L), eq("친한친구"), eq(true)))
                .willReturn(mockLabel);

        // when & then
        mockMvc.perform(post("/api/v1/labels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/labels/" + labelId))
                .andExpect(jsonPath("$.id").value(labelId)) // 문자열 비교
                .andExpect(jsonPath("$.labelName").value("친한친구"));
    }

    @Test
    @DisplayName("라벨을 삭제한다")
    void deleteLabel_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/labels/{labelId}", labelId))
                .andExpect(status().isNoContent());

        verify(labelService).deleteLabel(eq(1L), eq(labelId));
    }

    @Test
    @DisplayName("라벨의 이름을 변경하거나 노출 여부를 업데이트한다")
    void updateLabel_Success() throws Exception {
        // given
        String newLabelName = "새로운 라벨명";
        Boolean newExposure = false;
        LabelUpdateRequest dto = new LabelUpdateRequest(newLabelName, newExposure);

        // when & then
        mockMvc.perform(patch("/api/v1/labels/{labelId}", labelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(labelService).updateLabel(
                eq(labelId),
                eq(1L),
                eq(newLabelName),
                eq(newExposure)
        );
    }

    @Test
    @DisplayName("라벨의 멤버를 일괄 교체한다")
    void replaceMembers_Success() throws Exception {
        // given
        LabelMembersReplaceRequest dto = new LabelMembersReplaceRequest(List.of(2L, 3L));

        // when & then
        mockMvc.perform(put("/api/v1/labels/{labelId}/members", labelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(labelService).replaceLabelMembers(anyLong(),eq(labelId), eq(List.of(2L, 3L)));
    }

    @Test
    @DisplayName("라벨에 특정 멤버 하나를 추가한다")
    void addMember_Success() throws Exception {
        // given
        LabelMemberAddRequest dto = new LabelMemberAddRequest(2L);

        // when & then
        mockMvc.perform(post("/api/v1/labels/{labelId}/members", labelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(labelService).addMemberToLabel(anyLong(),eq(labelId), eq(2L));
    }

    @Test
    @DisplayName("라벨에서 특정 멤버를 제거한다")
    void removeMember_Success() throws Exception {
        // given
        Long memberId = 2L;

        // when & then
        mockMvc.perform(delete("/api/v1/labels/{labelId}/members/{memberId}", labelId, memberId))
                .andExpect(status().isNoContent());

        verify(labelService).removeMemberFromLabel(anyLong(),eq(labelId), eq(memberId));
    }
}