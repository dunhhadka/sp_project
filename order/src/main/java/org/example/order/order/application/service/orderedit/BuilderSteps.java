package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.model.orderedit.CalculatedLineItem;

import java.util.Map;

public interface BuilderSteps {


    interface Builder {
        BuildResult build();
    }

    interface BuildResult {
        CalculatedLineItem lineItem();

        Map<MergedTaxLine.TaxLineKey, MergedTaxLine> taxLines();
    }
}
