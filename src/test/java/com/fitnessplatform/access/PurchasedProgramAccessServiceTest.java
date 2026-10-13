package com.fitnessplatform.access;

import com.fitnessplatform.program.Program;
import com.fitnessplatform.purchase.Purchase;
import com.fitnessplatform.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchasedProgramAccessServiceTest {

    @Mock
    private ProgramAccessRepository programAccessRepository;

    private PurchasedProgramAccessService service;

    @BeforeEach
    void setUp() {
        service = new PurchasedProgramAccessService(
                programAccessRepository
        );
    }

    @Test
    void shouldGrantAccessForPaidPurchase() {
        Instant paidAt =
                Instant.parse("2026-10-13T12:00:00Z");

        Purchase purchase = paidPurchase(paidAt);

        when(programAccessRepository.findByPurchaseId(
                purchase.getId()
        )).thenReturn(Optional.empty());

        service.grantForPurchase(purchase);

        ArgumentCaptor<ProgramAccess> captor =
                ArgumentCaptor.forClass(ProgramAccess.class);

        verify(programAccessRepository)
                .saveAndFlush(captor.capture());

        ProgramAccess access = captor.getValue();

        assertSame(purchase, access.getPurchase());
        assertSame(purchase.getUser(), access.getUser());
        assertSame(purchase.getProgram(), access.getProgram());

        assertEquals(paidAt, access.getStartsAt());

        assertEquals(
                Instant.parse("2027-04-13T12:00:00Z"),
                access.getExpiresAt()
        );

        assertNull(access.getRevokedAt());
    }

    @Test
    void shouldRejectPendingPurchase() {
        Purchase purchase = pendingPurchase();

        assertThrows(
                IllegalStateException.class,
                () -> service.grantForPurchase(purchase)
        );

        verifyNoInteractions(programAccessRepository);
    }

    @Test
    void shouldRejectPaidPurchaseWithoutPaidAt() {
        Purchase purchase = mock(Purchase.class);

        when(purchase.getStatus())
                .thenReturn(
                        com.fitnessplatform.purchase.PurchaseStatus.PAID
                );

        when(purchase.getPaidAt())
                .thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> service.grantForPurchase(purchase)
        );

        verifyNoInteractions(programAccessRepository);
    }

    @Test
    void shouldNotCreateDuplicateAccess() {
        Instant paidAt =
                Instant.parse("2026-10-13T12:00:00Z");

        Purchase purchase = paidPurchase(paidAt);

        ProgramAccess existingAccess =
                mock(ProgramAccess.class);

        when(programAccessRepository.findByPurchaseId(
                purchase.getId()
        )).thenReturn(Optional.of(existingAccess));

        service.grantForPurchase(purchase);

        verify(programAccessRepository, never())
                .saveAndFlush(any(ProgramAccess.class));
    }

    @Test
    void shouldCalculateSixCalendarMonthsAtEndOfMonth() {
        Instant paidAt =
                Instant.parse("2026-08-31T12:00:00Z");

        Purchase purchase = paidPurchase(paidAt);

        service.grantForPurchase(purchase);

        ArgumentCaptor<ProgramAccess> captor =
                ArgumentCaptor.forClass(ProgramAccess.class);

        verify(programAccessRepository)
                .saveAndFlush(captor.capture());

        assertEquals(
                Instant.parse("2027-02-28T12:00:00Z"),
                captor.getValue().getExpiresAt()
        );
    }

    @Test
    void shouldRejectNullPurchase() {
        assertThrows(
                NullPointerException.class,
                () -> service.grantForPurchase(null)
        );

        verifyNoInteractions(programAccessRepository);
    }

    private Purchase pendingPurchase() {
        User user = mock(User.class);
        Program program = mock(Program.class);

        return new Purchase(
                user,
                program,
                8900L,
                "USD"
        );
    }

    private Purchase paidPurchase(Instant paidAt) {
        Purchase purchase = pendingPurchase();

        purchase.markPaid(
                paidAt,
                "pi_test_123"
        );

        return purchase;
    }
}