package org.example.order.order.application.model.orderedit;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.example.order.order.infrastructure.data.dto.LineItemDto;
import org.example.order.order.infrastructure.data.dto.OrderEditLineItemDto;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class CalculatedLineItem {
    private String id;

    private int quantity;

    private int editableQuantity;

    private int editableQuantityBeforeChange;

    private boolean restockable;
    private boolean restocking;

    private BigDecimal originalUnitPrice = BigDecimal.ZERO;
    private BigDecimal discountedUnitPrice = BigDecimal.ZERO;

    private BigDecimal editableSubtotal = BigDecimal.ZERO;
    private BigDecimal uneditableSubtotal = BigDecimal.ZERO;

    private boolean hasStagedLineItemDiscount;

    private List<CalculatedDiscountAllocation> calculatedDiscountAllocations;

    private List<OrderStagedChangeModel> stagedChanges;

    private String sku;
    private String title;
    private String variantTitle;
    private Integer variantId;

    private List<LineItemPropertyResponse> properties;

    public CalculatedLineItem(OrderEditLineItemDto lineItem) {

    }

    public CalculatedLineItem(LineItemDto lineItem) {

    }
}
