package com.fooddelivery.repository.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.fooddelivery.model.Delivery;

public class DeliveryRowMapper implements RowMapper<Delivery> {
    @Override
    public Delivery mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Delivery(
                rs.getInt("id"),
                rs.getInt("order_id"),
                rs.getInt("user_id"),
                rs.getString("status"),
                rs.getInt("partner_id")
        );
    }
}
