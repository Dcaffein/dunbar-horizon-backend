package com.example.DunbarHorizon.flag.adapter.out.persistence;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagInvitationJpaRepository;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitation;
import com.example.DunbarHorizon.flag.domain.invitation.FlagInvitationStatus;
import com.example.DunbarHorizon.flag.domain.invitation.repository.FlagInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class FlagInvitationRepositoryAdapter implements FlagInvitationRepository {

    private final FlagInvitationJpaRepository jpaRepository;

    @Override
    public FlagInvitation save(FlagInvitation invitation) {
        return jpaRepository.save(invitation);
    }

    @Override
    public List<FlagInvitation> saveAll(List<FlagInvitation> invitations) {
        return jpaRepository.saveAll(invitations);
    }

    @Override
    public List<FlagInvitation> findByInviteeId(Long inviteeId) {
        return jpaRepository.findAllByInviteeIdAndStatusOrderByCreatedAtDesc(inviteeId, FlagInvitationStatus.PENDING);
    }

    @Override
    public List<FlagInvitation> findByInviterId(Long inviterId) {
        return jpaRepository.findAllByInviterIdAndStatusOrderByCreatedAtDesc(inviterId, FlagInvitationStatus.PENDING);
    }

    @Override
    public Optional<FlagInvitation> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsPendingByFlagIdAndInviteeId(Long flagId, Long inviteeId) {
        return jpaRepository.existsByFlagIdAndInviteeIdAndStatus(flagId, inviteeId, FlagInvitationStatus.PENDING);
    }

    @Override
    public Set<Long> findPendingInviteeIdsByFlagId(Long flagId) {
        return jpaRepository.findPendingInviteeIdsByFlagId(flagId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void hardDeleteByFlagIdsIn(Collection<Long> flagIds) {
        jpaRepository.hardDeleteByFlagIdsIn(flagIds);
    }
}
