package com.parksmart.service;

import com.parksmart.dto.response.BookingResponse;
import com.parksmart.entity.*;
import com.parksmart.entity.Booking.BookingStatus;
import com.parksmart.entity.ParkingSlot.SlotStatus;
import com.parksmart.entity.ParkingSlot.SlotType;
import com.parksmart.entity.Vehicle.VehicleType;
import com.parksmart.event.BookingEventPublisher;
import com.parksmart.factory.PricingStrategyFactory;
import com.parksmart.factory.VehicleSlotFactory;
import com.parksmart.repository.*;
import com.parksmart.strategy.HourlyPricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingPaymentFlowTest {

    @Mock BookingRepository bookingRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock ParkingSlotRepository slotRepository;
    @Mock ParkingLotRepository lotRepository;
    @Mock BillRepository billRepository;
    @Mock VehicleSlotFactory vehicleSlotFactory;
    @Mock PricingStrategyFactory pricingStrategyFactory;
    @Mock BookingEventPublisher eventPublisher;
    @Mock PaymentRepository paymentRepository;

    private BookingService bookingService;
    private PaymentService paymentService;

    private UUID ownerId;
    private UUID bookingId;
    private Booking booking;
    private Bill bill;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
            bookingRepository,
            vehicleRepository,
            slotRepository,
            lotRepository,
            billRepository,
            vehicleSlotFactory,
            pricingStrategyFactory,
            eventPublisher
        );
        paymentService = new PaymentService(
            paymentRepository,
            billRepository,
            bookingRepository,
            eventPublisher
        );

        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", "sk_test_placeholder");
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "whsec_placeholder");
        ReflectionTestUtils.setField(paymentService, "successUrl", "http://localhost:3000/payment/success");
        ReflectionTestUtils.setField(paymentService, "cancelUrl", "http://localhost:3000/payment/cancel");

        ownerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        ParkingLot lot = ParkingLot.builder()
            .id(UUID.randomUUID())
            .name("Flow Lot")
            .address("1 Flow St")
            .active(true)
            .build();
        ParkingFloor floor = ParkingFloor.builder()
            .id(UUID.randomUUID())
            .floorName("Ground")
            .floorNumber(1)
            .parkingLot(lot)
            .build();
        ParkingSlot slot = ParkingSlot.builder()
            .id(UUID.randomUUID())
            .floor(floor)
            .slotNumber("M-01")
            .slotType(SlotType.MEDIUM)
            .status(SlotStatus.OCCUPIED)
            .build();
        Vehicle vehicle = Vehicle.builder()
            .id(UUID.randomUUID())
            .licensePlate("FLOW1")
            .vehicleType(VehicleType.CAR)
            .build();

        booking = Booking.builder()
            .id(bookingId)
            .user(User.builder().id(ownerId).build())
            .vehicle(vehicle)
            .slot(slot)
            .entryTime(LocalDateTime.now().minusMinutes(90))
            .status(BookingStatus.ACTIVE)
            .build();

        bill = Bill.builder()
            .id(UUID.randomUUID())
            .booking(booking)
            .baseAmount(new BigDecimal("8.00"))
            .taxAmount(new BigDecimal("0.80"))
            .totalAmount(new BigDecimal("8.80"))
            .pricingStrategy("HOURLY_FLAT")
            .build();
    }

    @Test
    void exitThenCreatePaymentSession_withExistingPaymentLink_reachesPaymentFlow() {
        Payment payment = Payment.builder()
            .id(UUID.randomUUID())
            .bill(bill)
            .amount(bill.getTotalAmount())
            .paymentLink("https://checkout.stripe.com/pay/existing")
            .status(Payment.PaymentStatus.PENDING)
            .build();

        when(bookingRepository.findById(bookingId))
            .thenReturn(Optional.of(booking))
            .thenReturn(Optional.of(booking));
        when(slotRepository.updateStatus(any(), eq(SlotStatus.AVAILABLE))).thenReturn(1);
        when(pricingStrategyFactory.resolveForTime(any())).thenReturn(new HourlyPricingStrategy());
        when(billRepository.save(any(Bill.class))).thenReturn(bill);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByBillId(bill.getId())).thenReturn(Optional.of(payment));
        doNothing().when(eventPublisher).publish(any());

        BookingResponse exitResponse = bookingService.exitVehicle(bookingId, ownerId);
        booking.setBill(bill);

        String checkoutLink = paymentService.createCheckoutSession(bookingId, ownerId, false);

        assertThat(exitResponse.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(exitResponse.getBill().getTotalAmount()).isEqualByComparingTo("8.80");
        assertThat(checkoutLink).isEqualTo("https://checkout.stripe.com/pay/existing");
        verify(paymentRepository).findByBillId(bill.getId());
    }
}
