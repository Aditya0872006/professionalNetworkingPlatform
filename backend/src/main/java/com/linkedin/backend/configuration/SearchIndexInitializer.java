package com.linkedin.backend.configuration;

import com.linkedin.backend.features.authentication.model.User;
import jakarta.persistence.EntityManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class SearchIndexInitializer {

    @Bean
    CommandLineRunner initializeSearchIndex(
            EntityManager entityManager,
            PlatformTransactionManager transactionManager) {

        return args -> {

            TransactionTemplate transactionTemplate =
                    new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(status -> {

                System.out.println("========== REBUILDING HIBERNATE SEARCH INDEX ==========");

                SearchSession searchSession = Search.session(entityManager);

                try {
                    searchSession
                            .massIndexer(User.class)
                            .startAndWait();

                    System.out.println("========== SEARCH INDEX REBUILT SUCCESSFULLY ==========");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Search index rebuild interrupted", e);
                }
            });
        };
    }
}