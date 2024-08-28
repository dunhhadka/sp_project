package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.application.service.customer.Customer;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcCustomerDao implements CustomerDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Customer getById(int storeId, int customerId) {
        var customer = jdbcTemplate.queryForObject(
                """
                        SELECT * 
                        FROM customers 
                        where store_id = :storeId AND id = :id 
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("id", customerId),
                BeanPropertyRowMapper.newInstance(Customer.class)
        );
        if (customer == null) throw new NotFoundException();
        return customer;
    }

    @Override
    public Customer getByEmail(int storeId, String email) {
        var customer = jdbcTemplate.queryForObject(
                """
                        SELECT * 
                        FROM customers 
                        where store_id = :storeId AND email = :email 
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("email", email),
                BeanPropertyRowMapper.newInstance(Customer.class)
        );
        if (customer == null) throw new NotFoundException();
        return customer;
    }

    @Override
    public Customer getByPhone(int storeId, String phone) {
        var customer = jdbcTemplate.queryForObject(
                """
                        SELECT * 
                        FROM customers 
                        where store_id = :storeId AND phone = :phone 
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("phone", phone),
                BeanPropertyRowMapper.newInstance(Customer.class)
        );
        if (customer == null) throw new NotFoundException();
        return customer;
    }
}
