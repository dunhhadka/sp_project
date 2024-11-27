package org.example.order.order.domain.draftorder.persistence;

import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.draftorder.model.DraftOrderNumber;
import org.example.order.order.infrastructure.data.dao.JpaDraftOrderNumberRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DBNumberGeneratorImpl implements NumberGenerator {

    private final JpaDraftOrderNumberRepository repository;

    @Override
    public int generateDraftNumber(int storeId) {
        var draftOrderNumberOptional = repository.findFirstByStoreId(storeId);
        DraftOrderNumber draftOrderNumber;
        int currentDraftOrderNumber = 1;
        if (draftOrderNumberOptional.isPresent() && draftOrderNumberOptional.get().getNextDraftOrderNumber() > 0) {
            draftOrderNumber = draftOrderNumberOptional.get();
            currentDraftOrderNumber = draftOrderNumber.getNextDraftOrderNumber();
            draftOrderNumber.update();
        } else {
            draftOrderNumber = new DraftOrderNumber(storeId);
        }
        repository.save(draftOrderNumber);
        return currentDraftOrderNumber;
    }
}
