// Interface CropRepository to perform database operations
package com.cropdeal.cropservice.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import com.cropdeal.cropservice.entity.Crop;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CropRepository extends JpaRepository<Crop, Long> {
    // Fetch crops in ascending order of price when filter is applied
    List<Crop> findByPriceBetween(Double minPrice, Double maxPrice, Sort sort);
    @Modifying
    @Transactional
    @Query(value = "UPDATE crops c JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS new_id FROM crops) t ON c.id = t.id SET c.id = t.new_id", nativeQuery = true)
    void reorderCropIds();

}
