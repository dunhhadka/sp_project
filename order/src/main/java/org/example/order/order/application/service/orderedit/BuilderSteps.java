package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.model.orderedit.CalculatedLineItem;

public interface BuilderSteps {


    interface Builder {
        BuildResult build();
    }

    interface BuildResult {
        CalculatedLineItem lineItem();
    }
}
