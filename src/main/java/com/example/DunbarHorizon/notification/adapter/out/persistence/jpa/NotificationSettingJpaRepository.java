package com.example.DunbarHorizon.notification.adapter.out.persistence.jpa;

import com.example.DunbarHorizon.notification.domain.NotificationSetting;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingJpaRepository extends JpaRepository<NotificationSetting, Long> {

    @Override
    @NonNull
    Optional<NotificationSetting> findById(@NonNull Long id);

    List<NotificationSetting> findAllByUserIdIn(List<Long> userIds);
}
