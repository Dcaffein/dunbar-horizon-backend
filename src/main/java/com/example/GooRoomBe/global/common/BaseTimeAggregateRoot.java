package com.example.GooRoomBe.global.common;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@MappedSuperclass
public abstract class BaseTimeAggregateRoot extends BaseTimeEntity {

    @Transient
    private final List<Object> domainEvents = new ArrayList<>();

    @DomainEvents
    protected Collection<Object> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @AfterDomainEventPublication
    protected void clearDomainEvents() {
        this.domainEvents.clear();
    }

    protected <T> T registerEvent(T event) {
        this.domainEvents.add(event);
        return event;
    }
}