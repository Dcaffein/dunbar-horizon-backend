package com.example.DunbarHorizon.notification.adapter.in.web;

import com.example.DunbarHorizon.global.event.notification.NotificationType;
import com.example.DunbarHorizon.notification.domain.Notification;
import com.example.DunbarHorizon.support.BaseControllerTest;
import com.example.DunbarHorizon.support.WithMockCustomUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest extends BaseControllerTest {

    @Test
    @WithMockCustomUser
    @DisplayName("알림 목록은 숫자 ID와 기존 필드, 메타데이터 값을 그대로 응답한다")
    void getMyNotifications_Success_NumericIdAndSameFields() throws Exception {
        // given
        Notification notification = notification(42L, Map.of("flagId", 7L, "nickname", "tester"));
        given(notificationService.getMyNotifications(eq(1L), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(notification)));

        // when & then
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].title").value("제목"))
                .andExpect(jsonPath("$.content[0].content").value("내용"))
                .andExpect(jsonPath("$.content[0].type").value("FLAG_INVITATION"))
                .andExpect(jsonPath("$.content[0].isRead").value(false))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].metadata.flagId").value(7))
                .andExpect(jsonPath("$.content[0].metadata.nickname").value("tester"));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("숫자 ID 경로로 알림을 읽음 처리한다")
    void readNotification_Success_NumericPath() throws Exception {
        // given
        Notification notification = notification(42L, null);
        notification.read(1L);
        given(notificationService.readNotification(42L, 1L)).willReturn(notification);

        // when & then
        mockMvc.perform(patch("/api/v1/notifications/42/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("숫자 ID 경로로 알림을 삭제한다")
    void deleteNotification_Success_NumericPath() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/notifications/42"))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(42L, 1L);
    }

    private Notification notification(Long id, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .receiverId(1L)
                .title("제목")
                .content("내용")
                .type(NotificationType.FLAG_INVITATION)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }
}
