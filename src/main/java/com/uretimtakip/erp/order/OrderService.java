package com.uretimtakip.erp.order;

import com.uretimtakip.erp.common.exception.BusinessException;
import com.uretimtakip.erp.common.exception.ResourceNotFoundException;
import com.uretimtakip.erp.order.dto.OrderItemRequest;
import com.uretimtakip.erp.order.dto.OrderRequest;
import com.uretimtakip.erp.order.dto.OrderResponse;
import com.uretimtakip.erp.part.PartRepository;
import com.uretimtakip.erp.projectbom.ProjectBomRepository;
import com.uretimtakip.erp.purchasing.PurchaseItemRepository;
import com.uretimtakip.erp.workorder.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order is mantigi. CRUD + items yonetimi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final WorkOrderRepository workOrderRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ProjectBomRepository projectBomRepository;

    @Transactional(readOnly = true)
    public List<OrderResponse> listAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        return OrderResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listByStatus(String status) {
        return orderRepository.findByStatus(status)
                .stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        if (orderRepository.existsByProjectName(request.getProjectName())) {
            throw new BusinessException(
                    "Bu proje adi zaten kullaniliyor: " + request.getProjectName(),
                    "PROJECT_NAME_EXISTS"
            );
        }

        Order order = Order.builder()
                .projectName(request.getProjectName())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .location(request.getLocation())
                .deliveryDays(request.getDeliveryDays())
                .totalPrice(request.getTotalPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "TRY")
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .approvedBy(request.getApprovedBy())
                .notes(request.getNotes())
                .build();

        // Items'lari ekle (varsa)
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (OrderItemRequest itemReq : request.getItems()) {
                OrderItem item = OrderItem.builder()
                        .order(order)
                        .itemName(itemReq.getItemName())
                        .description(itemReq.getDescription())
                        .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1)
                        .build();
                order.getItems().add(item);
            }
        }

        Order saved = orderRepository.save(order);
        log.info("Order created: id={}, project={}", saved.getId(), saved.getProjectName());

        return OrderResponse.fromEntity(saved);
    }

    @Transactional
    public OrderResponse update(UUID id, OrderRequest request) {
        Order order = findEntityById(id);
        String oldProjectName = order.getProjectName();

        // Eger proje adi degistiyse ve baska bir siparista ayni isim varsa hata
        if (!order.getProjectName().equals(request.getProjectName())
                && orderRepository.existsByProjectName(request.getProjectName())) {
            throw new BusinessException(
                    "Bu proje adi zaten kullaniliyor: " + request.getProjectName(),
                    "PROJECT_NAME_EXISTS"
            );
        }

        order.setProjectName(request.getProjectName());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setLocation(request.getLocation());
        order.setDeliveryDays(request.getDeliveryDays());
        order.setTotalPrice(request.getTotalPrice());
        if (request.getCurrency() != null) order.setCurrency(request.getCurrency());
        if (request.getStatus() != null) order.setStatus(request.getStatus());
        order.setApprovedBy(request.getApprovedBy());
        order.setNotes(request.getNotes());

        // Items: eski hepsini sil, yenilerini ekle (basit yaklasim)
        if (request.getItems() != null) {
            order.getItems().clear();
            for (OrderItemRequest itemReq : request.getItems()) {
                OrderItem item = OrderItem.builder()
                        .order(order)
                        .itemName(itemReq.getItemName())
                        .description(itemReq.getDescription())
                        .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1)
                        .build();
                order.getItems().add(item);
            }
        }

        Order updated = orderRepository.save(order);
        log.info("Order updated: id={}, project={}", updated.getId(), updated.getProjectName());

        // (K2) Proje adi degistiyse STRING ile bagli tablolar da ayni
        // transaction'da tasinir — yoksa satin alma / urun agaci baglantilari
        // eski ada takili kalir, proje ekranda ikiye bolunmus gorunur.
        if (!oldProjectName.equals(updated.getProjectName())) {
            int renamedItems = purchaseItemRepository.renameProjectName(
                    oldProjectName, updated.getProjectName());
            int renamedBoms = projectBomRepository.renameProjectName(
                    oldProjectName, updated.getProjectName());
            log.info("Project renamed '{}' -> '{}': purchase_items={}, project_bom={}",
                    oldProjectName, updated.getProjectName(), renamedItems, renamedBoms);
        }

        return OrderResponse.fromEntity(updated);
    }

    @Transactional
    public void delete(UUID id) {
        Order order = findEntityById(id);

        // (K1) Tek onayla tum proje verisinin gitmesini engelle:
        // parts/work_orders/departments CASCADE ile topluca silinirdi;
        // purchase_items/project_bom proje adini STRING tuttugundan silinmeyip
        // sahipsiz kalirdi. Bagli kaydi olan proje silinemez.
        long parts = partRepository.countByOrderId(id);
        long workOrders = workOrderRepository.countByOrderId(id);
        long purchaseItems = purchaseItemRepository.countByProjectName(order.getProjectName());
        long projectBoms = projectBomRepository.countByProjectName(order.getProjectName());
        if (parts + workOrders + purchaseItems + projectBoms > 0) {
            StringBuilder sb = new StringBuilder("Bu proje silinemez, bagli kayitlari var:");
            if (parts > 0) sb.append(" ").append(parts).append(" uretim parcasi,");
            if (workOrders > 0) sb.append(" ").append(workOrders).append(" is emri,");
            if (purchaseItems > 0) sb.append(" ").append(purchaseItems).append(" satin alma kalemi,");
            if (projectBoms > 0) sb.append(" ").append(projectBoms).append(" urun agaci baglantisi,");
            sb.setLength(sb.length() - 1);
            sb.append(". Once bunlari silin/tasiyin.");
            throw new BusinessException(sb.toString(), "ORDER_HAS_DEPENDENT_DATA");
        }

        orderRepository.delete(order);
        log.info("Order deleted: id={}, project={}", id, order.getProjectName());
    }

    private Order findEntityById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }
}