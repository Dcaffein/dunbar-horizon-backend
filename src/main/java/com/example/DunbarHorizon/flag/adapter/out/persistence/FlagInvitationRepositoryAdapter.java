package com.example.DunbarHorizon.flag.adapter.out.persistence;

import com.example.DunbarHorizon.flag.adapter.out.persistence.jpa.FlagInvitationJpaRepository;
import com.example.DunbarHorizon.flag.domain.flag.FlagInvitation;
import com.example.DunbarHorizon.flag.domain.flag.FlagInvitationStatus;
import com.example.DunbarHorizon.flag.domain.flag.repository.FlagInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FlagInvitationRepositoryAdapter implements FlagInvitationRepository {

    private final FlagInvitationJpaRepository jpaRepository;

    @Override
    public FlagInvitation save(FlagInvitation invitation) {
        return jpaRepository.save(invitation);
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
    public void hardDeleteByFlagIdsIn(Collection<Long> flagIds) {
        jpaRepository.hardDeleteByFlagIdsIn(flagIds);
    }
}
