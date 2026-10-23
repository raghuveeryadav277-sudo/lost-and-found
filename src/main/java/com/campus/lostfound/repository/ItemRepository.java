package com.campus.lostfound.repository;

import com.campus.lostfound.model.Item;
import com.campus.lostfound.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByReportedByOrderByCreatedAtDesc(User user);

    List<Item> findByTypeAndStatus(Item.Type type, Item.Status status);

    List<Item> findAllByOrderByCreatedAtDesc();

    /** One flexible search query: every filter is optional (null = ignore). */
    @Query("""
            SELECT i FROM Item i
            WHERE (:type IS NULL OR i.type = :type)
              AND (:status IS NULL OR i.status = :status)
              AND (:category IS NULL OR LOWER(i.category) = LOWER(:category))
              AND (:location IS NULL OR LOWER(i.location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:keyword IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                   OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY i.createdAt DESC
            """)
    List<Item> search(@Param("type") Item.Type type,
                      @Param("status") Item.Status status,
                      @Param("category") String category,
                      @Param("location") String location,
                      @Param("keyword") String keyword);

    long countByStatus(Item.Status status);
    long countByType(Item.Type type);
}
