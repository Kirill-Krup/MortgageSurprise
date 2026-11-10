package com.surprise.mortgagesurprise.repository.mongo;

import com.surprise.mortgagesurprise.model.document.PaymentScheduleDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentScheduleMongoRepository extends MongoRepository<PaymentScheduleDocument, String> {
    Optional<PaymentScheduleDocument> findByCalculationHash(String calculationHash);
}
