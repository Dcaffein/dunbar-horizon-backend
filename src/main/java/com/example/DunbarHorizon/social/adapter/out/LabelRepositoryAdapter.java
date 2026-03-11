package com.example.DunbarHorizon.social.adapter.out;

import com.example.DunbarHorizon.social.adapter.out.neo4j.springData.LabelNeo4jRepository;
import com.example.DunbarHorizon.social.domain.label.Label;
import com.example.DunbarHorizon.social.domain.label.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class LabelRepositoryAdapter implements LabelRepository {

    private final LabelNeo4jRepository labelNeo4jRepository;

    @Override
    public Label save(Label label) {
        return labelNeo4jRepository.save(label);
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
    public void delete(Label label) {
        labelNeo4jRepository.delete(label);
    }

    @Override
    public Optional<Label> findById(String labelId) {
        return labelNeo4jRepository.findById(labelId);
    }

    @Override
    public void saveAll(List<Label> labels) {
        labelNeo4jRepository.saveAll(labels);
    }

    @Override
    public Set<Long> findMemberIdsByOwnerAndLabelIds(Long ownerId, List<String> labelIds) {
        return labelNeo4jRepository.findMemberIdsByOwnerAndLabelIds(ownerId, labelIds);
    }
}
