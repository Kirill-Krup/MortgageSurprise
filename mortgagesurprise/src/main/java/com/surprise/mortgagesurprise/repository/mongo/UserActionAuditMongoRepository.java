package com.surprise.mortgagesurprise.repository.mongo;

import com.surprise.mortgagesurprise.model.document.UserActionAuditDocument;
import com.surprise.mortgagesurprise.model.enums.UserAuditActionType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserActionAuditMongoRepository extends MongoRepository<UserActionAuditDocument, String> {

    List<UserActionAuditDocument> findByUserIdOrderByTimestampDesc(UUID userId);

    List<UserActionAuditDocument> findByActionTypeOrderByTimestampDesc(UserAuditActionType actionType);

    List<UserActionAuditDocument> findAllByOrderByTimestampDesc();

    List<UserActionAuditDocument> findByBankOfferIdNotNullOrderByTimestampDesc();

    boolean existsByUserIdAndActionTypeIn(UUID userId, Collection<UserAuditActionType> actionTypes);

    Optional<UserActionAuditDocument> findFirstByUserIdAndApplicationIdAndActionTypeIn(
            UUID userId, String applicationId, Collection<UserAuditActionType> actionTypes);
}
