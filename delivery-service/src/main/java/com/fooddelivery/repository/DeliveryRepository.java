package com.fooddelivery.repository;

import java.util.List;

import com.fooddelivery.model.Delivery;

public interface DeliveryRepository {
    void save(Delivery delivery);
    void updateStatus(int id, String status, Integer partnerId);
    void delete(int id);
    Delivery findById(int id);
    List<Delivery> findAll();
}
