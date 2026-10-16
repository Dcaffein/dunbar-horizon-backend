package com.example.DunbarHorizon.social.adapter.out.persistence.neo4j;

import com.example.DunbarHorizon.social.adapter.out.persistence.neo4j.springData.LabelNeo4jRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import com.example.DunbarHorizon.social.domain.socialUser.exception.UserReferenceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class LabelRepositoryAdapter implements LabelRepository {

    private final LabelNeo4jRepository labelNeo4jRepository;
    private final Neo4jMappingContext mappingContext;

    @Override
    public void create(Label label) {
        Long ownerId = label.getOwner().getId();
        if (labelNeo4jRepository.create(ownerId, propertiesOf(label)) == 0L) {
            throw new UserReferenceNotFoundException(ownerId);
        }
    }

    @Override
    public void rename(String labelId, String labelName) {
        labelNeo4jRepository.rename(labelId, labelName);
    }

    @Override
    public void delete(String labelId) {
        labelNeo4jRepository.deleteById(labelId);
    }

    @Override
    public boolean existsMember(String labelId, Long memberId) {
        return labelNeo4jRepository.existsMember(labelId, memberId);
    }

    @Override
    public void addMember(String labelId, Long memberId) {
        if (labelNeo4jRepository.addMember(labelId, memberId) == 0L) {
            throw new UserReferenceNotFoundException(memberId);
        }
    }

    @Override
    public void removeMember(String labelId, Long memberId) {
        labelNeo4jRepository.removeMember(labelId, memberId);
    }

    @Override
    public void replaceMembers(String labelId, Collection<Long> memberIds) {
        labelNeo4jRepository.replaceMembers(labelId, memberIds);
    }

    @Override
    public void removeMemberFromOwnedLabels(Long ownerId, Long memberId) {
        labelNeo4jRepository.removeMemberFromOwnedLabels(ownerId, memberId);
    }

    @Override
    public boolean existsByOwner_IdAndLabelName(Long ownerId, String labelName) {
        return labelNeo4jRepository.existsByOwner_IdAndLabelName(ownerId, labelName);
    }

    @Override
    public List<Label> findAllByOwner_Id(Long ownerId) {
        return labelNeo4jRepository.findAllByOwner_Id(ownerId);
    }

    @Override
    public Optional<Label> findById(String labelId) {
        return labelNeo4jRepository.findById(labelId);
    }

    @Override
    public List<Label> findLabelsByOwnerAndMember(Long ownerId, Long memberId) {
        return labelNeo4jRepository.findLabelsByOwnerAndMember(ownerId, memberId);
    }

    @Override
    public Set<Long> findMemberIdsByOwnerAndLabelIds(Long ownerId, List<String> labelIds) {
        return labelNeo4jRepository.findMemberIdsByOwnerAndLabelIds(ownerId, labelIds);
    }

    /**
     * SDN 매핑이 정한 엔티티 자신의 속성만 꺼낸다. 관계와 연결 노드는 포함하지 않는다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> propertiesOf(Label label) {
        return (Map<String, Object>) mappingContext.getRequiredBinderFunctionFor(Label.class)
                .apply(label)
                .get(Constants.NAME_OF_PROPERTIES_PARAM);
    }
}
