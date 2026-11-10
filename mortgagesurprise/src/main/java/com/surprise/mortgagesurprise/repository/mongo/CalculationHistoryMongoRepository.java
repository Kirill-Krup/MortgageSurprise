package com.surprise.mortgagesurprise.repository.mongo;

import com.surprise.mortgagesurprise.model.document.CalculationHistoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CalculationHistoryMongoRepository extends MongoRepository<CalculationHistoryDocument, String> {
    List<CalculationHistoryDocument> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
