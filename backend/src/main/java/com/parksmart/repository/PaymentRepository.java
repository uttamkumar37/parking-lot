package com.parksmart.repository;

import com.parksmart.entity.Bill;
import com.parksmart.entity.Payment;
import com.parksmart.entity.Payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByStripeSessionId(String stripeSessionId);
    Optional<Payment> findByBillId(UUID billId);
    Optional<Payment> findByBillBookingId(UUID bookingId);
}
