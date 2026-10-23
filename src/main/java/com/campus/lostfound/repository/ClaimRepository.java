package com.campus.lostfound.repository;

import com.campus.lostfound.model.Claim;
import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByClaimantOrderByTimestampDesc(User claimant);
    List<Claim> findByItemReportedByOrderByTimestampDesc(User reporter);
    List<Claim> findAllByOrderByTimestampDesc();
    Optional<Claim> findByVerificationToken(String token);
    boolean existsByItemAndClaimantAndVerificationStatusIn(Item item, User claimant,
                                                           List<Claim.VerificationStatus> statuses);
}
