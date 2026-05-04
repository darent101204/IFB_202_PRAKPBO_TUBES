package com.example.project.repository;

import com.example.project.model.Region;
import com.example.project.model.Rt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RtRepository extends JpaRepository<Rt, Long> {
    List<Rt> findByRegion(Region region);
    List<Rt> findByRegionId(Long regionId);
}
