package com.snapshop.repository;

import com.snapshop.model.entity.ImportBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

	List<ImportBatch> findTop20ByOrderByIdDesc();

}
