package org.example.order.application.service.fulfillmentorder;

import org.example.order.SapoClient;
import org.example.order.domain.fulfillmentorder.model.FulfillmentOrderFixtures;
import org.example.order.infrastructure.persistence.InMemoryIdGenerator;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.model.fulfillmentorder.request.FulfillmentOrderMoveRequest;
import org.example.order.order.application.service.fulfillment.FulfillmentOrderWriteService;
import org.example.order.order.application.service.fulfillmentorder.FulfillmentOrderHelperService;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FulfillmentOrderWriteServiceTest implements FulfillmentOrderFixtures {

    FulfillmentOrderWriteService fulfillmentOrderWriteService;
    InMemoryIdGenerator idGenerator;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private FulfillmentOrderRepository fulfillmentOrderRepository;
    @Mock
    private FulfillmentOrderHelperService fulfillmentOrderHelperService;
    @MockBean
    private ApplicationEventPublisher applicationEventPublisher;
    @MockBean
    private MessageSource messageSource;
    @Mock
    private SapoClient sapoClient;


    @BeforeEach
    void setUp() {
        idGenerator = new InMemoryIdGenerator();
        fulfillmentOrderWriteService = new FulfillmentOrderWriteService(
                idGenerator, orderRepository, fulfillmentOrderRepository,
                fulfillmentOrderHelperService, applicationEventPublisher, sapoClient);
    }

    @Test
    void moving_the_open_fulfillment_Order_only_change_the_assigned_location() {
        var fulfillmentOrder = defaultFulfillmentOrder();
        var order = defaultOrder();
        var locationsForMove = locationsForMove();
        var destination = destinationLocation();

        when(fulfillmentOrderRepository.findById(fulfillmentOrder.getId())).thenReturn(Optional.of(fulfillmentOrder));
        when(orderRepository.findById(any())).thenReturn(order);
        when(fulfillmentOrderHelperService.getLocationForMove(fulfillmentOrder.getId())).thenReturn(locationsForMove);

        when(sapoClient.location(any())).thenReturn(destination);

        var fulfillmentOrderMovedRequest = FulfillmentOrderMoveRequest.builder()
                .newLocationId(destination.getId())
                .build();

        fulfillmentOrderWriteService.move(fulfillmentOrder.getId(), fulfillmentOrderMovedRequest);

        var fulfillmentOrderCaptor = ArgumentCaptor.forClass(FulfillmentOrder.class);

        verify(fulfillmentOrderRepository, times(1)).save(fulfillmentOrderCaptor.capture());

        var movedFulfillmentOrder = fulfillmentOrderCaptor.getAllValues().get(0);

        assertEquals(fulfillmentOrder.getId(), movedFulfillmentOrder.getId());
        assertEquals(FulfillmentOrder.FulfillmentOrderStatus.open, movedFulfillmentOrder.getStatus());
        assertEquals(fulfillmentOrder.getAssignedLocationId(), movedFulfillmentOrder.getAssignedLocationId());
        assertEquals(fulfillmentOrder.getAssignedLocation(), movedFulfillmentOrder.getAssignedLocation());
    }

    @ParameterizedTest
    @ValueSource(ints = {123, 234, 345, 456, 789})
    void the_destination_location_should_be_in_the_location_for_move_and_marked_as_movable(int destinationLocation) {
        var fulfillmentOrder = defaultFulfillmentOrder();
        var order = defaultOrder();
        var locationsForMove = locationsForMove();

        when(fulfillmentOrderRepository.findById(fulfillmentOrder.getId())).thenReturn(Optional.of(fulfillmentOrder));
        when(orderRepository.findById(any())).thenReturn(order);

        when(fulfillmentOrderHelperService.getLocationForMove(fulfillmentOrder.getId())).thenReturn(locationsForMove);

        var fulfillmentOrderMoveRequest = FulfillmentOrderMoveRequest.builder()
                .newLocationId((long) destinationLocation)
                .build();
        var exception = assertThrows(ConstrainViolationException.class, () -> fulfillmentOrderWriteService.move(fulfillmentOrder.getId(), fulfillmentOrderMoveRequest));

        assertEquals(1, exception.getErrorMessage().getUserErrors().size());
        var userError = exception.getErrorMessage().getUserErrors().get(0);
        assertEquals("not_allowed", userError.getCode());
        assertTrue(userError.getFields().contains("location_id"));
    }

    @Test
    void moving_location_with_partial_fulfillment_order_and_create_new_fulfillment_order() {
        var fulfillmentOrder = partialFulfillmentLocation();
        var order = defaultOrder();
        var locationsForMove = locationsForMove();
        var destination = destinationLocation();

        var lineItem1RemainingQuantity = fulfillmentOrder.getLineItems().get(0).getRemainingQuantity();
        var lineItem2RemainingQuantity = fulfillmentOrder.getLineItems().get(1).getRemainingQuantity();
        var lineItem3RemainingQuantity = fulfillmentOrder.getLineItems().get(2).getRemainingQuantity();

        when(fulfillmentOrderRepository.findById(fulfillmentOrder.getId())).thenReturn(Optional.of(fulfillmentOrder));
        when(orderRepository.findById(any())).thenReturn(order);
        when(fulfillmentOrderHelperService.getLocationForMove(fulfillmentOrder.getId())).thenReturn(locationsForMove);

        when(sapoClient.location(any())).thenReturn(destination);

        var fulfillmentOrderMoveRequest = FulfillmentOrderMoveRequest.builder()
                .newLocationId(destination.getId())
                .build();

        fulfillmentOrderWriteService.move(fulfillmentOrder.getId(), fulfillmentOrderMoveRequest);

        var fulfillmentOrderCaptor = ArgumentCaptor.forClass(FulfillmentOrder.class);
        verify(fulfillmentOrderRepository, times(2)).save(fulfillmentOrderCaptor.capture());

        var savedOriginalFulfillmentOrder = fulfillmentOrderCaptor.getAllValues().get(0);
        var movedFulfillmentOrder = fulfillmentOrderCaptor.getAllValues().get(1);

        assertEquals(FulfillmentOrder.FulfillmentOrderStatus.closed, savedOriginalFulfillmentOrder.getStatus());
        assertEquals(0, savedOriginalFulfillmentOrder.getLineItems().get(0).getRemainingQuantity());
        assertEquals(0, savedOriginalFulfillmentOrder.getLineItems().get(1).getRemainingQuantity());
        assertEquals(0, savedOriginalFulfillmentOrder.getLineItems().get(2).getRemainingQuantity());

        assertEquals(FulfillmentOrder.FulfillmentOrderStatus.open, movedFulfillmentOrder.getStatus());
        assertEquals(lineItem1RemainingQuantity, movedFulfillmentOrder.getLineItems().get(0).getRemainingQuantity());
        assertEquals(lineItem2RemainingQuantity, movedFulfillmentOrder.getLineItems().get(1).getRemainingQuantity());
        assertEquals(lineItem3RemainingQuantity, movedFulfillmentOrder.getLineItems().get(2).getRemainingQuantity());
    }
}
