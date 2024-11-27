package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.model.orderedit.CalculatedLineItem;

import java.util.Map;

/**
 * output là BuilderResult
 */
public interface BuilderSteps {

    interface Builder {
        BuilderResult build();
    }

    interface BuilderResult {
        CalculatedLineItem lineItem();

        Map<MergedTaxLine.TaxLineKey, MergedTaxLine> taxLines();
    }
}
