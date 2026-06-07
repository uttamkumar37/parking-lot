package com.parksmart.controller;

import com.parksmart.entity.User;
import com.parksmart.security.JwtUtil;
import com.parksmart.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PaymentService paymentService;

    @MockBean
    JwtUtil jwtUtil;

    @Test
    void getPaymentStatus_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/payments/{bookingId}/status", UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getPaymentStatus_forDifferentUser_returnsForbidden() throws Exception {
        UUID bookingId = UUID.randomUUID();
        User userA = userA();

        when(paymentService.getPaymentStatus(eq(bookingId), eq(userA.getId()), eq(false)))
            .thenThrow(new AccessDeniedException("Booking does not belong to this user"));

        mockMvc.perform(get("/payments/{bookingId}/status", bookingId).with(user(userA)))
            .andExpect(status().isForbidden());
    }

    @Test
    void createCheckout_forDifferentUser_returnsForbidden() throws Exception {
        UUID bookingId = UUID.randomUUID();
        User userA = userA();

        when(paymentService.createCheckoutSession(eq(bookingId), eq(userA.getId()), eq(false)))
            .thenThrow(new AccessDeniedException("Booking does not belong to this user"));

        mockMvc.perform(post("/payments/{bookingId}/checkout", bookingId)
                .with(user(userA))
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    private User userA() {
        return User.builder()
            .id(UUID.randomUUID())
            .name("User A")
            .email("a@example.com")
            .password("password")
            .role(User.Role.USER)
            .active(true)
            .build();
    }
}
