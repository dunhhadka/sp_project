package org.example.order.order.application.model.draftorder.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DraftOrderCreateRequest extends DraftOrderRequest {
    @NotEmpty
    private @Size(max = 250) List<@Valid DraftLineItemRequest> lineItems = new ArrayList<>();
}
