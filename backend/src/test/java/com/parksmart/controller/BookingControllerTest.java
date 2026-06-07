package com.parksmart.controller;

import com.parksmart.entity.User;
import com.parksmart.security.JwtUtil;
import com.parksmart.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    BookingService bookingService;

    @MockBean
    JwtUtil jwtUtil;

    @Test
    void getBooking_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/bookings/{bookingId}", UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getBooking_forDifferentUser_returnsForbidden() throws Exception {
        UUID bookingId = UUID.randomUUID();
        User userA = User.builder()
            .id(UUID.randomUUID())
            .name("User A")
            .email("a@example.com")
            .password("password")
            .role(User.Role.USER)
            .active(true)
            .build();

        when(bookingService.getBooking(eq(bookingId), eq(userA.getId()), eq(false)))
            .thenThrow(new AccessDeniedException("Booking does not belong to this user"));

        mockMvc.perform(get("/bookings/{bookingId}", bookingId).with(user(userA)))
            .andExpect(status().isForbidden());
    }
}
