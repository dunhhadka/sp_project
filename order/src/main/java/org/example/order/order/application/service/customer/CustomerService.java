package org.example.order.order.application.service.customer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.exception.NotFoundException;
import org.example.order.order.domain.order.model.MailingAddress;
import org.example.order.order.infrastructure.data.dao.CustomerDao;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerDao customerDao;

    public Customer findById(int storeId, int customerId) {
        if (customerId <= 0) return null;
        return withNotFoundHandler(() -> customerDao.getById(storeId, customerId));
    }

    private <V> V withNotFoundHandler(Supplier<V> supplier) {
        try {
            return supplier.get();
        } catch (NotFoundException e) {
            log.error("customer not found");
        }
        return null;
    }

    public Customer findByEmail(int storeId, String email) {
        if (StringUtils.isBlank(email)) return null;
        return withNotFoundHandler(() -> customerDao.getByEmail(storeId, email));
    }

    public Customer findByPhone(int storeId, String phone) {
        return withNotFoundHandler(() ->
                customerDao.getByPhone(storeId, phone));
    }

    public Customer create(
            int storeId,
            String email,
            String phone,
            Pair<String, String> fullName,
            MailingAddress address
    ) {
        return new Customer();
    }

    public Customer update(int storeId, int id, String email, String phone) {
        return new Customer();
    }
}
