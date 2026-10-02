package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccess;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProgramWeekUnlockServiceTest {

    @Test
    void shouldUnlockWeekOneAtAccessStart() {
        ProgramAccess access =
                accessStartingAt(
                        "2026-10-01T12:00:00Z"
                );

        ProgramWeek week =
                weekAtPosition(1);

        ProgramWeekUnlockService service =
                serviceAt(
                        "2026-10-01T12:00:00Z"
                );

        assertTrue(
                service.isUnlocked(
                        access,
                        week
                )
        );
    }

    @Test
    void shouldKeepWeekOneLockedBeforeAccessStart() {
        ProgramAccess access =
                accessStartingAt(
                        "2026-10-01T12:00:00Z"
                );

        ProgramWeek week =
                weekAtPosition(1);

        ProgramWeekUnlockService service =
                serviceAt(
                        "2026-10-01T11:59:59Z"
                );

        assertFalse(
                service.isUnlocked(
                        access,
                        week
                )
        );
    }

    @Test
    void shouldKeepWeekTwoLockedBeforeSevenDays() {
        ProgramAccess access =
                accessStartingAt(
                        "2026-10-01T12:00:00Z"
                );

        ProgramWeek week =
                weekAtPosition(2);

        ProgramWeekUnlockService service =
                serviceAt(
                        "2026-10-08T11:59:59Z"
                );

        assertFalse(
                service.isUnlocked(
                        access,
                        week
                )
        );
    }

    @Test
    void shouldUnlockWeekTwoExactlyAfterSevenDays() {
        ProgramAccess access =
                accessStartingAt(
                        "2026-10-01T12:00:00Z"
                );

        ProgramWeek week =
                weekAtPosition(2);

        ProgramWeekUnlockService service =
                serviceAt(
                        "2026-10-08T12:00:00Z"
                );

        assertTrue(
                service.isUnlocked(
                        access,
                        week
                )
        );
    }

    @Test
    void shouldUnlockWeekThreeAfterFourteenDays() {
        ProgramAccess access =
                accessStartingAt(
                        "2026-10-01T12:00:00Z"
                );

        ProgramWeek week =
                weekAtPosition(3);

        ProgramWeekUnlockService service =
                serviceAt(
                        "2026-10-15T12:00:00Z"
                );

        assertTrue(
                service.isUnlocked(
                        access,
                        week
                )
        );
    }

    @Test
    void shouldCalculateWeekSixUnlockDate() {
        ProgramAccess access =
                accessStartingAt(
                        "2026-10-01T12:00:00Z"
                );

        ProgramWeek week =
                weekAtPosition(6);

        ProgramWeekUnlockService service =
                serviceAt(
                        "2026-10-01T12:00:00Z"
                );

        Instant unlocksAt =
                service.calculateUnlocksAt(
                        access,
                        week
                );

        assertEquals(
                Instant.parse(
                        "2026-11-05T12:00:00Z"
                ),
                unlocksAt
        );
    }

    @Test
    void shouldCalculateUnlockForArbitraryWeekPosition() {
        ProgramAccess access =
                accessStartingAt(
                        "2026-10-01T12:00:00Z"
                );

        ProgramWeek week =
                weekAtPosition(10);

        ProgramWeekUnlockService service =
                serviceAt(
                        "2026-10-01T12:00:00Z"
                );

        Instant unlocksAt =
                service.calculateUnlocksAt(
                        access,
                        week
                );

        assertEquals(
                Instant.parse(
                        "2026-12-03T12:00:00Z"
                ),
                unlocksAt
        );
    }

    private ProgramAccess accessStartingAt(
            String startsAt
    ) {
        ProgramAccess access =
                mock(ProgramAccess.class);

        when(
                access.getStartsAt()
        ).thenReturn(
                Instant.parse(startsAt)
        );

        return access;
    }

    private ProgramWeek weekAtPosition(
            int position
    ) {
        ProgramWeek week =
                mock(ProgramWeek.class);

        when(
                week.getPosition()
        ).thenReturn(position);

        return week;
    }

    private ProgramWeekUnlockService serviceAt(
            String now
    ) {
        Clock clock =
                Clock.fixed(
                        Instant.parse(now),
                        ZoneOffset.UTC
                );

        return new ProgramWeekUnlockService(
                clock
        );
    }
}