package com.fooddelivery.repository;

import java.util.List;

import com.fooddelivery.model.Partner;

public interface PartnerRepository {
    void save(Partner partner);
    void update(Partner partner);
    void delete(int id);
    Partner findById(int id);
    List<Partner> findAll();
}
