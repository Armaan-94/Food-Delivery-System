package com.fooddelivery.repository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.fooddelivery.model.Delivery;
import com.fooddelivery.repository.mapper.DeliveryRowMapper;
import com.fooddelivery.exceptions.DatabaseAccessException;
import com.fooddelivery.exceptions.ResourceNotFoundException;

@Repository
public class DeliveryRepositoryImpl implements DeliveryRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void save(Delivery delivery) {
        try {
            String sql = "INSERT INTO deliveries(order_id, user_id, status, partner_id) VALUES (?, ?, ?, ?)";
            int rows = jdbcTemplate.update(sql, delivery.getOrderId(), delivery.getUserId(),
                    delivery.getStatus(), delivery.getAssignedPartnerId());
            if (rows == 0) throw new DatabaseAccessException("Delivery save failed, no rows affected.");
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to save delivery", e);
        }
    }

    @Override
    public void updateStatus(int id, String status, Integer partnerId) {
        try {
            String sql = "UPDATE deliveries SET status=?, partner_id=? WHERE id=?";
            int rows = jdbcTemplate.update(sql, status, partnerId, id);
            if (rows == 0) throw new ResourceNotFoundException("Delivery update failed, not found with id: " + id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to update delivery status for id: " + id, e);
        }
    }

    @Override
    public void delete(int id) {
        try {
            String sql = "DELETE FROM deliveries WHERE id=?";
            int rows = jdbcTemplate.update(sql, id);
            if (rows == 0) throw new ResourceNotFoundException("Delivery delete failed, not found with id: " + id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to delete delivery with id: " + id, e);
        }
    }

    @Override
    public Delivery findById(int id) {
        try {
            String sql = "SELECT * FROM deliveries WHERE id=?";
            Delivery delivery = jdbcTemplate.queryForObject(sql, new DeliveryRowMapper(), id);
            if (delivery == null) throw new ResourceNotFoundException("Delivery not found with id: " + id);
            return delivery;
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException("Delivery not found with id: " + id);
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to find delivery by id: " + id, e);
        }
    }

    @Override
    public List<Delivery> findAll() {
        try {
            String sql = "SELECT * FROM deliveries";
            return jdbcTemplate.query(sql, new DeliveryRowMapper());
        } catch (Exception e) {
            throw new DatabaseAccessException("Failed to find all deliveries", e);
        }
    }
}
