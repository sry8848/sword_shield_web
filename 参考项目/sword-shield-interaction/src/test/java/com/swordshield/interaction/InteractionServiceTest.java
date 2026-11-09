package com.swordshield.interaction;

import com.swordshield.interaction.repository.CommentRepository;
import com.swordshield.interaction.repository.LikeRepository;
import com.swordshield.interaction.repository.ShareRepository;
import com.swordshield.interaction.service.InteractionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

public class InteractionServiceTest {
    @Test
    void testLikeAndUnlike() {
        CommentRepository cr = Mockito.mock(CommentRepository.class);
        LikeRepository lr = Mockito.mock(LikeRepository.class);
        ShareRepository sr = Mockito.mock(ShareRepository.class);
        InteractionService service = new InteractionService(cr, lr, sr);

        Mockito.when(lr.findByUserIdAndTargetTypeAndTargetId(1L, "article", 2L)).thenReturn(Optional.empty());
        Mockito.when(lr.countByTargetTypeAndTargetId("article", 2L)).thenReturn(1L);
        long count = service.like(1L, "article", 2L);
        Assertions.assertEquals(1L, count);

        Mockito.when(lr.findByUserIdAndTargetTypeAndTargetId(1L, "article", 2L)).thenReturn(Optional.of(new com.swordshield.interaction.domain.Like()));
        Mockito.when(lr.countByTargetTypeAndTargetId("article", 2L)).thenReturn(0L);
        long c2 = service.unlike(1L, "article", 2L);
        Assertions.assertEquals(0L, c2);
    }
}

