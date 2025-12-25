package com.swordshield.activity;

import com.swordshield.activity.domain.Activity;
import com.swordshield.activity.repository.ActivityRegistrationRepository;
import com.swordshield.activity.repository.ActivityRepository;
import com.swordshield.activity.service.ActivityService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

public class ActivityServiceTest {
    @Test
    void testRegisterTimeWindowAndCapacity() {
        ActivityRepository ar = Mockito.mock(ActivityRepository.class);
        ActivityRegistrationRepository rr = Mockito.mock(ActivityRegistrationRepository.class);
        ActivityService service = new ActivityService(ar, rr);

        Instant now = Instant.now();
        Activity act = new Activity();
        act.setId(10L);
        act.setStartTime(now.plusSeconds(3600));
        act.setEndTime(now.plusSeconds(7200));
        act.setMaxRegistrations(1);
        Mockito.when(ar.findById(10L)).thenReturn(Optional.of(act));
        Mockito.when(rr.findByActivityIdAndUserId(Mockito.eq(10L), Mockito.anyLong())).thenReturn(Optional.empty());
        Mockito.when(rr.countByActivityId(10L)).thenReturn(0L);

        service.register(1L, 10L, "web");

        Mockito.when(rr.countByActivityId(10L)).thenReturn(1L);
        Assertions.assertThrows(IllegalArgumentException.class, () -> service.register(2L, 10L, "web"));
    }
}

