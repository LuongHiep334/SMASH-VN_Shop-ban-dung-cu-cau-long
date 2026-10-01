package com.smashvn.shop.service;

import com.smashvn.shop.entity.NewsletterSubscriber;
import com.smashvn.shop.exception.NewsletterValidationException;
import com.smashvn.shop.repository.NewsletterSubscriberRepository;
import com.smashvn.shop.dao.DotGiamGiaDAO;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.service.impl.NewsletterServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsletterServiceTest {

    @Mock
    private NewsletterSubscriberRepository subscriberRepository;
    @Mock
    private DotGiamGiaDAO dotGiamGiaDAO;
    @Mock
    private PhieuGiamGiaRepository phieuGiamGiaRepository;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private KhachHangRepository khachHangRepository;

    @InjectMocks
    private NewsletterServiceImpl newsletterService;

    @Mock
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void testSubscribe_NewActiveUser() {
        String email = "Test@Example.Com";
        when(subscriberRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        newsletterService.subscribe(email);

        verify(subscriberRepository, times(1)).save(any(NewsletterSubscriber.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSubscribe_AlreadyActiveUser_ThrowsException() {
        String email = "test@example.com";
        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setEmail(email);
        subscriber.setTrangThai("hoat_dong");

        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(subscriber));

        NewsletterValidationException ex = assertThrows(NewsletterValidationException.class, () -> {
            newsletterService.subscribe(email);
        });

        assertEquals("Email này đã đăng ký nhận ưu đãi.", ex.getMessage());
        verify(subscriberRepository, never()).save(any());
    }

    @Test
    void testSubscribe_UnsubscribedUser_Reactivates() {
        String email = "test@example.com";
        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setEmail(email);
        subscriber.setTrangThai("da_huy");

        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(subscriber));

        newsletterService.subscribe(email);

        assertEquals("hoat_dong", subscriber.getTrangThai());
        assertNull(subscriber.getNgayHuy());
        assertNotNull(subscriber.getTokenHuy());

        verify(subscriberRepository, times(1)).save(subscriber);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSubscribe_InvalidEmail_ThrowsException() {
        String email = "invalid-email";
        assertThrows(NewsletterValidationException.class, () -> {
            newsletterService.subscribe(email);
        });
    }

    @Test
    void testUnsubscribe_ValidToken() {
        String token = "my-token";
        NewsletterSubscriber subscriber = new NewsletterSubscriber();
        subscriber.setEmail("test@example.com");
        subscriber.setTrangThai("hoat_dong");
        subscriber.setTokenHuy(token);

        when(subscriberRepository.findByTokenHuy(token)).thenReturn(Optional.of(subscriber));

        newsletterService.unsubscribe(token);

        assertEquals("da_huy", subscriber.getTrangThai());
        assertNotNull(subscriber.getNgayHuy());
        verify(subscriberRepository, times(1)).save(subscriber);
    }

    @Test
    void testUnsubscribe_InvalidToken_ThrowsException() {
        String token = "invalid-token";
        when(subscriberRepository.findByTokenHuy(token)).thenReturn(Optional.empty());

        assertThrows(NewsletterValidationException.class, () -> {
            newsletterService.unsubscribe(token);
        });
    }
}
