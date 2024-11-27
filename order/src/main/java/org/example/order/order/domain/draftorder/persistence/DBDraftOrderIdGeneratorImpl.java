package org.example.order.order.domain.draftorder.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DBDraftOrderIdGeneratorImpl implements DraftOrderIdGenerator {

    @Override
    public int generateDraftOrderId() {
        return 0;
    }
}
