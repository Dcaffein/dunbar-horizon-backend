## Objective
Buzz의 수평적이고 자유로운 수다 성격을 반영하고 시스템 내 다른 도메인(Flag)과의 용어 일관성을 맞추기 위해 Reply(답장) 관련 용어를 Comment(댓글)로 리팩토링하여 유비쿼터스 언어를 확립합니다.

## Domain Change
[x] 있음 - Buzz 및 관련 값 객체의 명칭과 필드명 변경

## Target Files
- `buzz/domain/BuzzComment.java` - 클래스명을 BuzzComment로 변경
- `buzz/domain/Buzz.java` - 필드 및 메서드 명칭 변경
- `buzz/application/service/BuzzService.java` - 비즈니스 로직 내 용어 변경
- `buzz/application/dto/result/BuzzCommentResult.java` - 클래스명 변경
- `buzz/adapter/in/web/dto/BuzzCommentRequest.java` - 클래스명 변경
- `buzz/adapter/in/web/BuzzController.java` - 엔드포인트 및 메서드 호출부 수정

## Requirements
1. BuzzReply 클래스를 BuzzComment로 이름을 변경합니다.
2. Buzz 엔티티 내부의 replies 리스트 필드명을 comments로 변경합니다.
3. 답장 추가 메서드인 addReply 또는 createReply를 addComment 또는 createComment로 변경합니다.
4. 댓글 작성자를 지칭하던 replier 관련 변수 및 필드명을 commenter로 변경합니다.
5. 관련 DTO와 테스트 코드 내의 모든 Reply 키워드를 Comment로 치환합니다.

## Decisions
| 항목 | 결정값 | 비고 |
|------|--------|------|
| 대상 용어 | Comment / comments | 기존 Reply 대체 |
| 행위자 용어 | commenter | 기존 replier 대체 |
| 영향 범위 | 도메인, 애플리케이션, 어댑터 전체 | 언어 통일을 위한 전역 리팩토링 |

## API Contract Change
[x] 있음
- 변경 전: POST /api/v1/buzzes/{buzzId}/replies
- 변경 후: POST /api/v1/buzzes/{buzzId}/comments

## Testing Strategy
- Unit: 리팩토링 후 기존 테스트 코드의 모든 참조가 Comment로 변경되어 정상적으로 컴파일 및 수행되는지 확인
- Integration: API 경로 변경(/comments)에 맞춰 통합 테스트를 수정하고 응답 데이터 구조가 유지되는지 검증