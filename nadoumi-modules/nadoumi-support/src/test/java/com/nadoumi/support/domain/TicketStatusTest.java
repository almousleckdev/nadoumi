package com.nadoumi.support.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.nadoumi.support.domain.enums.TicketStatus;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TicketStatusTest {

    @Test
    void shouldFollowTheDocumentedMatrix_whenMovingBetweenStatuses() {
        assertThat(TicketStatus.OPEN.allowedNext()).containsExactly(TicketStatus.IN_PROGRESS);
        assertThat(TicketStatus.IN_PROGRESS.allowedNext())
                .containsExactlyInAnyOrder(TicketStatus.WAITING_ON_STUDENT, TicketStatus.RESOLVED);
        assertThat(TicketStatus.WAITING_ON_STUDENT.allowedNext())
                .containsExactlyInAnyOrder(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);
        assertThat(TicketStatus.RESOLVED.allowedNext()).containsExactly(TicketStatus.CLOSED);
    }

    @Test
    void shouldNeverReopen_whenTicketIsClosed() {
        assertThat(TicketStatus.CLOSED.allowedNext()).isEmpty();
    }

    @Test
    void shouldRejectSkippingStraightToClosedOrResolved_whenTicketIsStillOpen() {
        Set<TicketStatus> rejected = EnumSet.of(TicketStatus.WAITING_ON_STUDENT, TicketStatus.RESOLVED,
                TicketStatus.CLOSED, TicketStatus.OPEN);
        for (TicketStatus target : rejected) {
            assertThat(TicketStatus.OPEN.canMoveTo(target)).as("OPEN -> %s", target).isFalse();
        }
    }
}
