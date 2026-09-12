package com.example.DunbarHorizon.flag.domain.memorial;



import com.example.DunbarHorizon.global.exception.ErrorContext;import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.memorial.event.MemorialCreatedEvent;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialInvalidContentException;
import com.example.DunbarHorizon.global.common.BaseTimeAggregateRoot;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "flag_memorials")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlagMemorial extends BaseTimeAggregateRoot {

    public static final int CONTENT_MAX_LENGTH = 1000;
    public static final String CONTENT_LENGTH_MESSAGE = "내용은 1자 이상 1000자 이하로 작성해주세요.";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long flagId;
    private Long writerId;

    @Column(length = CONTENT_MAX_LENGTH, nullable = false)
    private String content;

    FlagMemorial(Long flagId, Long writerId, String content) {
        validateContent(content);
        this.flagId = flagId;
        this.writerId = writerId;
        this.content = content;
        registerEvent(new MemorialCreatedEvent(flagId));
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > CONTENT_MAX_LENGTH) {
            throw new FlagMemorialInvalidContentException();
        }
    }

    private void validateOwner(Long requesterId) {
        if (!this.writerId.equals(requesterId)) {
            throw new FlagAuthorizationException(FlagErrorCode.FLAG_MEMORIAL_AUTHOR_ONLY, ErrorContext.of("memorialId", id).and("userId", requesterId));
        }
    }

    public void updateContent(Long requesterId, String newContent) {
        validateOwner(requesterId);
        validateContent(newContent);
        this.content = newContent;
    }

    public void validateDeletion(Long requesterId) {
        validateOwner(requesterId);
    }
}