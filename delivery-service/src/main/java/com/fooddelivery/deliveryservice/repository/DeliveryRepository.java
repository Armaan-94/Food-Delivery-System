package com.fooddelivery.deliveryservice.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import com.fooddelivery.common.exception.DatabaseAccessException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.deliveryservice.model.Delivery;
import com.fooddelivery.deliveryservice.model.DeliveryStatus;

@Repository
public class DeliveryRepository {

    private static final RowMapper<Delivery> ROW_MAPPER = (rs, rowNum) -> {
        Delivery delivery = new Delivery();
        delivery.setId(rs.getLong("id"));
        delivery.setOrderId(rs.getLong("order_id"));
        // getObject keeps a NULL partner_id as null; getLong would turn it into 0.
        delivery.setPartnerId(rs.getObject("partner_id", Long.class));
        delivery.setStatus(DeliveryStatus.valueOf(rs.getString("status")));
        delivery.setPickupLocation(rs.getString("pickup_location"));
        delivery.setDropoffLocation(rs.getString("dropoff_location"));
        delivery.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        delivery.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return delivery;
    };

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insert;

    public DeliveryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("deliveries")
                .usingColumns("order_id", "partner_id", "status", "pickup_location", "dropoff_location")
                .usingGeneratedKeyColumns("id");
    }

    public List<Delivery> findAll() {
        return run("load deliveries", () -> jdbcTemplate.query("SELECT * FROM deliveries ORDER BY id", ROW_MAPPER));
    }

    public Optional<Delivery> findById(long id) {
        return run("load delivery " + id,
                () -> jdbcTemplate.query("SELECT * FROM deliveries WHERE id = ?", ROW_MAPPER, id).stream().findFirst());
    }

    /** Inserts the delivery and returns its generated id. */
    public long insert(Delivery delivery) {
        return run("create delivery", () -> {
            Map<String, Object> columns = new HashMap<>();
            columns.put("order_id", delivery.getOrderId());
            columns.put("partner_id", delivery.getPartnerId());
            columns.put("status", delivery.getStatus().name());
            columns.put("pickup_location", delivery.getPickupLocation());
            columns.put("dropoff_location", delivery.getDropoffLocation());
            return insert.executeAndReturnKey(columns).longValue();
        });
    }

    /** Returns false when no delivery has that id. */
    public boolean updateStatus(long id, DeliveryStatus status, Long partnerId) {
        return run("update delivery " + id, () -> jdbcTemplate.update(
                "UPDATE deliveries SET status = ?, partner_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                status.name(), partnerId, id) > 0);
    }

    /** Returns false when no delivery has that id. */
    public boolean deleteById(long id) {
        return run("delete delivery " + id, () -> jdbcTemplate.update("DELETE FROM deliveries WHERE id = ?", id) > 0);
    }

    private <T> T run(String action, Supplier<T> work) {
        try {
            return work.get();
        } catch (DuplicateKeyException e) {
            throw new DuplicateResourceException("A delivery already exists for this order.");
        } catch (DataAccessException e) {
            throw new DatabaseAccessException("Failed to " + action, e);
        }
    }
}
