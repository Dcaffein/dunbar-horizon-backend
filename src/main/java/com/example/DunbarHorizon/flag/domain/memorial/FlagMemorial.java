package com.example.DunbarHorizon.flag.domain.memorial;



import com.example.DunbarHorizon.global.exception.ErrorContext;
import com.example.DunbarHorizon.flag.domain.exception.FlagErrorCode;
import com.example.DunbarHorizon.flag.domain.flag.Flag;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagAuthorizationException;
import com.example.DunbarHorizon.flag.domain.flag.exception.FlagInvalidStatusException;
import com.example.DunbarHorizon.flag.domain.memorial.exception.FlagMemorialAuthorizationException;
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

    private FlagMemorial(Long flagId, Long writerId, String content) {
        validateContent(content);
        this.flagId = flagId;
        this.writerId = writerId;
        this.content = content;
    }

    public static FlagMemorial create(Flag flag, Long writerId, String content, boolean isParticipant) {
        if (!flag.isEnded()) {
            throw new FlagInvalidStatusException(FlagErrorCode.FLAG_NOT_ENDED,
                    ErrorContext.of("flagId", flag.getId()));
        }
        if (!flag.getHostId().equals(writerId) && !isParticipant) {
            throw new FlagMemorialAuthorizationException(FlagErrorCode.FLAG_MEMORIAL_PARTICIPANT_ONLY,
                    ErrorContext.of("flagId", flag.getId()).and("userId", writerId));
        }
        return new FlagMemorial(flag.getId(), writerId, content);
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
