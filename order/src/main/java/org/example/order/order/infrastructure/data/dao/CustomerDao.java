package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.application.service.customer.Customer;

public interface CustomerDao {

    Customer getById(int storeId, int customerId);

    Customer getByEmail(int storeId, String email);

    Customer getByPhone(int storeId, String phone);
}
