package io.rosecloud.notice.service.impl;

import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.common.core.error.CommonErrorCode;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.notice.channel.NoticeDispatchService;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeServiceImplTest {

    @Test
    void rejectsPushChannelsWithoutRecipients() {
        NoticeRepository repository = mock(NoticeRepository.class);
        when(repository.insert(any(Notice.class))).thenReturn(1L);
        NoticeDispatchService dispatchService = mock(NoticeDispatchService.class);
        NoticeServiceImpl service = new NoticeServiceImpl(repository, dispatchService);

        NoticePublishRequest request = new NoticePublishRequest(
                "title",
                "content",
                NoticeTargetType.GLOBAL.code(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                io.rosecloud.notice.domain.NoticeChannel.EMAIL.code(),
                List.of());

        assertThatThrownBy(() -> service.publish(request))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorCode())
                .isEqualTo(CommonErrorCode.PARAM_INVALID);

        verify(repository, never()).insert(any(Notice.class));
        verify(dispatchService, never()).dispatch(any(Notice.class));
    }
}
