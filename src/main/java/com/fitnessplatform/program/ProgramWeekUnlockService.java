package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccess;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ProgramWeekUnlockService {

    private static final long DAYS_PER_WEEK = 7;

    private final Clock clock;

    public ProgramWeekUnlockService(
            Clock clock
    ) {
        this.clock = clock;
    }

    public Instant calculateUnlocksAt(
            ProgramAccess access,
            ProgramWeek week
    ) {
        long daysToAdd =
                (long) (week.getPosition() - 1)
                        * DAYS_PER_WEEK;

        return access
                .getStartsAt()
                .plus(
                        daysToAdd,
                        ChronoUnit.DAYS
                );
    }

    public boolean isUnlocked(
            ProgramAccess access,
            ProgramWeek week
    ) {
        Instant now =
                clock.instant();

        Instant unlocksAt =
                calculateUnlocksAt(
                        access,
                        week
                );

        return !now.isBefore(
                unlocksAt
        );
    }

    public void requireUnlocked(
            ProgramAccess access,
            ProgramWeek week
    ) {
        Instant unlocksAt =
                calculateUnlocksAt(
                        access,
                        week
                );

        Instant now =
                clock.instant();

        if (
                now.isBefore(
                        unlocksAt
                )
        ) {
            throw new ProgramWeekLockedException(
                    week.getId(),
                    unlocksAt
            );
        }
    }
}
