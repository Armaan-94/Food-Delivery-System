package com.fooddelivery.repository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.fooddelivery.model.Partner;
import com.fooddelivery.repository.mapper.PartnerRowMapper;
import com.fooddelivery.exceptions.DatabaseAccessException;
import com.fooddelivery.exceptions.ResourceNotFoundException;

@Repository
public class PartnerRepositoryImpl implements PartnerRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void save(Partner partner) {
        try {
            String sql = "INSERT INTO partners(name, vehicle, availability) VALUES (?, ?, ?)";
            int rows = jdbcTemplate.update(sql, partner.getName(), partner.getVehicle(), partner.isAvailable());
            if (rows == 0) throw new DatabaseAccessException("Partner save failed, no rows affected.");
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to save partner", e);
        }
    }

    @Override
    public void update(Partner partner) {
        try {
            String sql = "UPDATE partners SET name=?, vehicle=?, availability=? WHERE id=?";
            int rows = jdbcTemplate.update(sql, partner.getName(), partner.getVehicle(), partner.isAvailable(), partner.getId());
            if (rows == 0) throw new ResourceNotFoundException("Partner update failed, not found with id: " + partner.getId());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to update partner with id: " + partner.getId(), e);
        }
    }

    @Override
    public void delete(int id) {
        try {
            String sql = "DELETE FROM partners WHERE id=?";
            int rows = jdbcTemplate.update(sql, id);
            if (rows == 0) throw new ResourceNotFoundException("Partner delete failed, not found with id: " + id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to delete partner with id: " + id, e);
        }
    }

    @Override
    public Partner findById(int id) {
        try {
            String sql = "SELECT * FROM partners WHERE id=?";
            Partner partner = jdbcTemplate.queryForObject(sql, new PartnerRowMapper(), id);
            if (partner == null) throw new ResourceNotFoundException("Partner not found with id: " + id);
            return partner;
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException("Partner not found with id: " + id);
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to find partner by id: " + id, e);
        }
    }

    @Override
    public List<Partner> findAll() {
        try {
            String sql = "SELECT * FROM partners";
            return jdbcTemplate.query(sql, new PartnerRowMapper());
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to find all partners", e);
        }
    }
}
