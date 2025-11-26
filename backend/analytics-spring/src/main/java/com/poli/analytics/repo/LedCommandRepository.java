package com.poli.analytics.repo;

import com.poli.analytics.model.LedCommand;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface LedCommandRepository extends MongoRepository<LedCommand, String> {
    List<LedCommand> findByTsAfterOrderByTsDesc(Instant since);
    List<LedCommand> findTop100ByOrderByTsDesc();
}
