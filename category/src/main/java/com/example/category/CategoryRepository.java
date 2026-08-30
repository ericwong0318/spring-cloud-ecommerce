package com.example.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentId(Long parentId);

    List<Category> findByParentIsNull();

    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.parent.id IN (SELECT c2.id FROM Category c2 WHERE c2.id = :ancestorId)")
    boolean existsAncestorDescendant(@Param("id") Long id, @Param("ancestorId") Long ancestorId);

    Optional<Category> findByIdWithChildren(Long id);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.id = :id")
    Optional<Category> findByIdFetchChildren(Long id);
}