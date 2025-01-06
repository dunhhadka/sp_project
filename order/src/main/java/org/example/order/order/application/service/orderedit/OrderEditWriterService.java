package org.example.order.order.application.service.orderedit;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.orderedit.model.OrderEditId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderEditWriterService {

    private final BeginOrderEditProcessor beginOrderEditProcessor;

    @Transactional
    public OrderEditId beginEdit(int storeId, int id) {
        return beginOrderEditProcessor.beginEdit(storeId, id);
    }
}
