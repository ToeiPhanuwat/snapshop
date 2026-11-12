package com.snapshop.repository;

import com.snapshop.model.entity.ProductType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {

	List<ProductType> findByCategoryCode(String categoryCode);

	Optional<ProductType> findByCode(String code);

}
