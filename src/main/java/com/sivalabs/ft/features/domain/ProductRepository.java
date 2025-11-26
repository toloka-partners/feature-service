package com.sivalabs.ft.features.domain;

import com.sivalabs.ft.features.domain.entities.Product;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

public interface ProductRepository extends ListCrudRepository<Product, Long> {
    Optional<Product> findByCode(String code);
}
