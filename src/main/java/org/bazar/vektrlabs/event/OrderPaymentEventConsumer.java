package org.bazar.vektrlabs.event;

import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.bazar.vektrlabs.cart.CartService;
import org.bazar.vektrlabs.dto.response.OrderResponseDto;

import org.bazar.vektrlabs.entity.enums.OrderStatus;
import org.bazar.vektrlabs.service.OrderService;
import org.bazar.vektrlabs.service.ProductVariantService;
import org.bazar.vektrlabs.util.CurrentUserUtil;
import org.jericho.notifications.dto.NotificationRequestDto;
import org.jericho.notifications.entity.NotificationStatus;
import org.jericho.notifications.entity.NotificationType;
import org.jericho.notifications.service.NotificationService;
import org.jericho.payment.event.PaymentEvent;
import org.jericho.payment.event.PaymentEventConsumer;
import org.jericho.payment.event.PaymentEventException;
import org.jericho.payment.event.PaymentStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentEventConsumer implements PaymentEventConsumer {

    private final OrderService orderService;
    private final ProductVariantService productVariantService;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final CurrentUserUtil currentUserUtil;

    @Override
    public boolean supports(PaymentEvent event) {
        return event != null && event.getMetadata() != null && event.getMetadata().containsKey("orderId");
    }

    // I should find a way of just passing the orderid or response dto to the client side, I found a dirty hack around this
    @Override
    public void consume(PaymentEvent event) {
        log.info("[OrderCheckoutPaymentEventConsumer.consume] Consuming payment event: {}", event.getEventId());
        Map<String, String> metadata = event.getMetadata();
        var orderId = UUID.fromString(metadata.getOrDefault("orderId",null));
        var cartId = UUID.fromString(metadata.getOrDefault("cartId",""));
        try {
            switch (event.getStatus()){
                case PaymentStatus.SUCCEEDED -> handlePaymentSuccess(orderId,cartId);
                case PaymentStatus.FAILED -> handlePaymentFailure(orderId);
                case PaymentStatus.CANCELLED ->  handlePaymentCancellation(orderId);
                default -> log.warn("[OrderCheckoutPaymentEventConsumer.consume] Unhandled payment status: {} for orderId: {}", event.getStatus(), orderId);
            }
            log.info("[OrderCheckoutPaymentEventConsumer.consume] Payment event processed successfully for orderId: {}", orderId);

        } catch (Exception e) {
            log.error("[OrderCheckoutPaymentEventConsumer.consume] Error processing payment event for orderId: {}", orderId, e);
            throw new PaymentEventException("Failed to process payment event", e);
        }
    }

    private void handlePaymentSuccess(UUID orderId, UUID cartId) {
        log.info("[OrderCheckoutPaymentEventConsumer.handlePaymentSuccess] Processing successful payment for orderId: {}", orderId);
        OrderResponseDto orderResponseDto = orderService.updateOrderStatus(orderId, OrderStatus.PAID);
        orderResponseDto.getItems().forEach(item ->
            productVariantService.updateVariantStock(item.getVariantId(), item.getQuantity())
        );
        cartService.clearAsSystem(cartId);
        sendNotification(buildNotificationRequestDto(orderResponseDto, "Your order has been successfully processed."));
    }

    private void handlePaymentFailure(UUID orderId) {
        log.warn("[OrderCheckoutPaymentEventConsumer.handlePaymentFailure] Processing failed payment for orderId: {}", orderId);
        OrderResponseDto orderResponseDto = orderService.updateOrderStatus(orderId, OrderStatus.FAILED);
        sendNotification(buildNotificationRequestDto(orderResponseDto, "Your order has failed due to payment issues."));
    }

    private void handlePaymentCancellation(UUID orderId) {
        log.warn("[OrderCheckoutPaymentEventConsumer.handlePaymentCancellation] Processing cancelled payment for orderId: {}", orderId);
        OrderResponseDto orderResponseDto = orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
        sendNotification(buildNotificationRequestDto(orderResponseDto, "Your order has failed due to payment issues."));
    }

    private void sendNotification(NotificationRequestDto notificationRequestDto){
        var response = notificationService.create(notificationRequestDto);
        if(!response.getStatus().equals(NotificationStatus.SENT)){
            // we need to handle this error accordingly to the end user.. maybe send it with the orderId param
            log.error("[OrderCheckoutPaymentEventConsumer.sendNotification] Failed to send notification: {}", response);
        }
    }

    private NotificationRequestDto buildNotificationRequestDto(OrderResponseDto orderResponseDto, String message){
        return NotificationRequestDto.builder()
                .recipient(currentUserUtil.currentEmailClaim())
                .subject(String.format("Order status for %s is %s", orderResponseDto.getId(), orderResponseDto.getStatus().name()))
                .type(NotificationType.EMAIL)
                .message(message)
                .build();
    }
}