package com.fooddelivery.repository.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.fooddelivery.model.Partner;

public class PartnerRowMapper implements RowMapper<Partner> {
    @Override
    public Partner mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Partner(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("vehicle"),
                rs.getBoolean("availability")
        );
    }
}
