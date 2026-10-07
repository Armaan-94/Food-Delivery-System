package com.fooddelivery.deliveryservice.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import com.fooddelivery.common.exception.DatabaseAccessException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.deliveryservice.model.Partner;

@Repository
public class PartnerRepository {

    private static final RowMapper<Partner> ROW_MAPPER = (rs, rowNum) -> {
        Partner partner = new Partner();
        partner.setId(rs.getLong("id"));
        partner.setName(rs.getString("name"));
        partner.setPhoneNumber(rs.getString("phone_number"));
        partner.setVehicleType(rs.getString("vehicle_type"));
        partner.setAvailable(rs.getBoolean("available"));
        partner.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return partner;
    };

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insert;

    public PartnerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("partners")
                .usingColumns("name", "phone_number", "vehicle_type", "available")
                .usingGeneratedKeyColumns("id");
    }

    public List<Partner> findAll() {
        return run("load partners", () -> jdbcTemplate.query("SELECT * FROM partners ORDER BY id", ROW_MAPPER));
    }

    public Optional<Partner> findById(long id) {
        return run("load partner " + id,
                () -> jdbcTemplate.query("SELECT * FROM partners WHERE id = ?", ROW_MAPPER, id).stream().findFirst());
    }

    /** Inserts the partner and returns its generated id. */
    public long insert(Partner partner) {
        return run("create partner", () -> {
            Map<String, Object> columns = new HashMap<>();
            columns.put("name", partner.getName());
            columns.put("phone_number", partner.getPhoneNumber());
            columns.put("vehicle_type", partner.getVehicleType());
            columns.put("available", partner.isAvailable());
            return insert.executeAndReturnKey(columns).longValue();
        });
    }

    /** Returns false when no partner has that id. */
    public boolean update(Partner partner) {
        return run("update partner " + partner.getId(), () -> jdbcTemplate.update(
                "UPDATE partners SET name = ?, phone_number = ?, vehicle_type = ?, available = ? WHERE id = ?",
                partner.getName(), partner.getPhoneNumber(), partner.getVehicleType(), partner.isAvailable(),
                partner.getId()) > 0);
    }

    /** Returns false when no partner has that id. */
    public boolean deleteById(long id) {
        try {
            return run("delete partner " + id, () -> jdbcTemplate.update("DELETE FROM partners WHERE id = ?", id) > 0);
        } catch (DatabaseAccessException e) {
            if (e.getCause() instanceof DataIntegrityViolationException) {
                throw new DuplicateResourceException("Partner is assigned to deliveries and cannot be deleted.");
            }
            throw e;
        }
    }

    private <T> T run(String action, Supplier<T> work) {
        try {
            return work.get();
        } catch (DataAccessException e) {
            throw new DatabaseAccessException("Failed to " + action, e);
        }
    }
}
