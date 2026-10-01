package com.smashvn.shop.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.user.TemporaryPasswordService.TemporaryPasswordIssueResult;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TemporaryPasswordService.class, TemporaryPasswordConcurrencyDataJpaTest.PasswordTestConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TemporaryPasswordConcurrencyDataJpaTest {

    @TestConfiguration
    static class PasswordTestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @MockitoBean
    private JavaMailSender mailSender;

    @jakarta.annotation.Resource
    private TaiKhoanRepository taiKhoanRepository;

    @jakarta.annotation.Resource
    private TemporaryPasswordService temporaryPasswordService;

    @jakarta.annotation.Resource
    private PasswordEncoder passwordEncoder;

    @Test
    void twoDifferentSepayOrdersPaidConcurrentlyIssueExactlyOneTemporaryPassword() throws Exception {
        TaiKhoan guest = new TaiKhoan();
        guest.setUsername("sepay-concurrency-" + UUID.randomUUID() + "@example.com");
        guest.setVaiTro("KH");
        guest.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        guest.setMatKhau(null);
        guest.setSoLanMuaThanhCong(0);
        guest = taiKhoanRepository.saveAndFlush(guest);
        Integer accountId = guest.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<TemporaryPasswordIssueResult> webhookOrderA = executor.submit(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return temporaryPasswordService.recordSepayPaymentSuccess(accountId);
            });
            Future<TemporaryPasswordIssueResult> webhookOrderB = executor.submit(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return temporaryPasswordService.recordSepayPaymentSuccess(accountId);
            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<TemporaryPasswordIssueResult> results = List.of(
                    webhookOrderA.get(15, TimeUnit.SECONDS),
                    webhookOrderB.get(15, TimeUnit.SECONDS));

            assertEquals(1, results.stream().filter(TemporaryPasswordIssueResult::isIssued).count());
            TemporaryPasswordIssueResult issued = results.stream()
                    .filter(TemporaryPasswordIssueResult::isIssued)
                    .findFirst()
                    .orElseThrow();

            TaiKhoan reloaded = taiKhoanRepository.findById(accountId).orElseThrow();
            assertEquals(2, reloaded.getSoLanMuaThanhCong());
            assertEquals(AccountStatus.GUEST, reloaded.getTrangThaiTaiKhoan());
            assertNotNull(reloaded.getMatKhau());
            assertTrue(passwordEncoder.matches(issued.temporaryPassword(), reloaded.getMatKhau()));
        } finally {
            start.countDown();
            executor.shutdownNow();
            taiKhoanRepository.deleteById(accountId);
        }
    }
}
