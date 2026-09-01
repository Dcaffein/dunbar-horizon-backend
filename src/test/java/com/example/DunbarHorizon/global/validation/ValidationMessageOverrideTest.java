package com.example.DunbarHorizon.global.validation;

import com.example.DunbarHorizon.notification.adapter.in.web.dto.DeviceTokenRequest;
import com.example.DunbarHorizon.trace.adapter.in.web.dto.VisitRequestDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationMessageOverrideTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("인라인 message가 없는 제약은 ValidationMessages.properties의 한국어 기본 문구로 떨어진다")
    void defaultMessage_IsOverriddenToKorean() {
        // VisitRequestDto.targetId 는 @NotNull 만 있고 인라인 message 가 없다
        var violations = validator.validate(new VisitRequestDto(null));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("필수 입력값입니다.");
    }

    @Test
    @DisplayName("빈 device token은 인라인 문구로 거절된다")
    void blankDeviceToken_IsRejected() {
        var violations = validator.validate(new DeviceTokenRequest("  "));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("디바이스 토큰은 필수입니다.");
    }
}
