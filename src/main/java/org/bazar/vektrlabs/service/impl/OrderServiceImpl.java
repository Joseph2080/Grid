package org.bazar.vektrlabs.service.impl;

import org.bazar.vektrlabs.dto.request.OrderItemRequestDto;
import org.bazar.vektrlabs.entity.OrderItem;
import org.bazar.vektrlabs.entity.ProductVariant;
import org.bazar.vektrlabs.entity.enums.Currency;
import org.bazar.vektrlabs.entity.enums.OrderStatus;
import org.bazar.vektrlabs.service.ProductVariantService;
import org.bazar.vektrlabs.service.UserProfileService;
import org.jericho.common.exception.EntityNotFoundException;
import org.jericho.common.service.AbstractJpaService;
import org.bazar.vektrlabs.dto.request.OrderCommand;
import org.bazar.vektrlabs.dto.response.OrderResponseDto;
import org.bazar.vektrlabs.entity.Order;
import org.bazar.vektrlabs.exception.OrderNotFoundException;
import org.bazar.vektrlabs.mapper.OrderMapper;
import org.bazar.vektrlabs.repository.OrderRepository;
import org.bazar.vektrlabs.service.OrderService;
import org.jericho.payment.dto.PaymentRequestDto;
import org.jericho.payment.dto.PaymentResponseDto;
import org.jericho.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.jericho.common.exception.InvalidParameterException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderServiceImpl extends AbstractJpaService<
        Order,
        UUID,
        OrderCommand,
        OrderResponseDto,
        OrderRepository>
        implements OrderService {

    private final ProductVariantService productVariantService;
    private final PaymentService paymentService;
    private final UserProfileService userProfileService;

    @Value("${payment.strategy:STRIPE}")
    private String PAYMENT_STRATEGY;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public OrderServiceImpl(
            OrderRepository repository,
            OrderMapper dtoMapper,
            PaymentService paymentService,
            ProductVariantService productVariantService,
            UserProfileService userProfileService) {
        super(repository, dtoMapper);
        this.paymentService = paymentService;
        this.productVariantService = productVariantService;
        this.userProfileService = userProfileService;
    }

    // add additional logs here too nigga
    @Override
    @Transactional
    public OrderResponseDto create(OrderCommand orderRequestDto){
        OrderResponseDto orderResponseDto = super.create(orderRequestDto);
        PaymentRequestDto paymentRequestDto = buildPaymentRequestDto(orderResponseDto);
        PaymentResponseDto paymentResponseDto = paymentService.createPayment(paymentRequestDto, PAYMENT_STRATEGY);
        orderResponseDto.setPaymentUrl(paymentResponseDto.getPaymentUrl());
        // payment url must be persisted properly
        return orderResponseDto;
    }

    @Override
    protected void setEntityDependencies(Order entity, OrderCommand requestDTO) {
        var buyer = userProfileService.requireCompleteProfileByCognitoSub(requestDTO.buyerCognitoSub());
        if (entity.getBuyer() != null && !entity.getBuyer().getId().equals(buyer.getId())) {
            throw new AccessDeniedException("An order's buyer cannot be changed.");
        }
        entity.setBuyer(buyer);
        var items = createOrderItems(entity, requestDTO);
        // Order.items is @OneToMany(orphanRemoval = true); entity.getItems() is the
        // Hibernate-managed persistent collection. Mutate it in place (clear + addAll)
        // instead of entity.setItems(items) - replacing the reference would break
        // orphan removal (old items wouldn't be deleted, or Hibernate would throw
        // "collection with cascade=all-delete-orphan was no longer referenced").
        entity.getItems().clear();
        entity.getItems().addAll(items);
        entity.setTotalAmount(calculateTotalAmount(items));
        entity.setCurrency(resolveOrderCurrency(items));
    }

    @Override
    protected void applyCustomValidation(OrderCommand command) {
        if (!StringUtils.hasText(command.buyerCognitoSub()) || command.cartId() == null
                || command.items() == null || command.items().isEmpty()) {
            throw new InvalidParameterException("A buyer, cart and order items are required.");
        }
        if (command.items().stream().anyMatch(item -> item == null || item.getVariantId() == null
                || item.getQuantity() == null || item.getQuantity() < 1)) {
            throw new InvalidParameterException("Order items require a variant and a positive quantity.");
        }
    }

    @Override
    @Transactional
    public OrderResponseDto update(UUID id, OrderCommand command) {
        var buyer = userProfileService.requireCompleteProfileByCognitoSub(command.buyerCognitoSub());
        var existing = findEntityByIdOrElseThrowException(id);
        if (!existing.getBuyer().getId().equals(buyer.getId())) {
            throw new AccessDeniedException("An order's buyer cannot be changed.");
        }
        if (!existing.getCartId().equals(command.cartId())) {
            throw new InvalidParameterException("An order's cart cannot be changed.");
        }
        return super.update(id, command);
    }

    private List<OrderItem> createOrderItems(Order entity, OrderCommand requestDTO){
        return  requestDTO
                .items()
                .stream()
                .map(orderItemRequestDto -> toOrderItem(entity, orderItemRequestDto))
                .toList();
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(UUID orderId, OrderStatus orderStatus){
        Order orderById = findEntityByIdOrElseThrowException(orderId);
        orderById.setStatus(orderStatus);
        orderById.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        repository.save(orderById);
        return dtoMapper.convertEntityToResponseDto(orderById);
    }

    private OrderItem toOrderItem(Order order, OrderItemRequestDto itemDto){
        // we also need to skip products if not available as well because the exception
        // will block the entire order creation. We can log the exception and skip the item.
        ProductVariant variant = productVariantService
                .findAvailableVariant(itemDto.getVariantId(), itemDto.getQuantity());
        return OrderItem.builder()
                .order(order)
                .variant(variant)
                .unitPrice(variant.getProduct().getPrice())
                .quantity(itemDto.getQuantity())
                .createdAt(LocalDateTime.now(ZoneId.systemDefault()))
                .updatedAt(LocalDateTime.now(ZoneId.systemDefault()))
                .build();
    }

    private Currency resolveOrderCurrency(List<OrderItem> items) {
        var distinctStoreIds = items.stream()
                .map(item -> item.getVariant().getProduct().getStore().getId())
                .distinct()
                .toList();
        if (distinctStoreIds.size() > 1) {
            throw new InvalidParameterException("An order cannot contain items from multiple stores.");
        }
        return items.get(0).getVariant().getProduct().getStore().getCurrency();
    }

    private BigDecimal calculateTotalAmount(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items
                .stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PaymentRequestDto buildPaymentRequestDto(OrderResponseDto orderResponseDto) {
        String baseUrl = normalizedAppBaseUrl();
        return PaymentRequestDto.builder()
                .amount(orderResponseDto.getTotalAmount())
                .currency(orderResponseDto.getCurrency().name())
                .successUrl(baseUrl + "/api/v1/payments/callback?success=true")
                .cancelUrl(baseUrl + "/api/v1/payments/callback?success=false")
                .metadata(buildPaymentMetadata(orderResponseDto))
                .build();
    }

    private Map<String, String> buildPaymentMetadata(OrderResponseDto orderResponseDto) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", orderResponseDto.getId().toString());
        metadata.put("cartId", orderResponseDto.getCartId().toString());
        String redirectUrl =
                normalizedAppBaseUrl() + "/Vektrlabs?orderId="
                        + orderResponseDto.getId();
        metadata.put("redirectUrl", redirectUrl);
        return metadata;
    }

    private String normalizedAppBaseUrl() {
        String baseUrl = appBaseUrl == null ? "http://localhost:8080" : appBaseUrl.trim();
        if (baseUrl.isEmpty()) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    protected EntityNotFoundException entityNotFoundException() {
        return new OrderNotFoundException(
                "order can not be found with the provided id"
        );
    }
}