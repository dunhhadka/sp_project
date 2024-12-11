package org.example.order.order.application.service.orderedit;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({AddService.class, ChangeQuantityService.class, TaxService.class})
public class CommitServiceConfiguration {
}
