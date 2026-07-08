package io.rosecloud.notice.service.impl;

import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.common.security.context.CurrentUser;
import io.rosecloud.common.security.context.UserContext;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticeChannel;
import io.rosecloud.notice.domain.NoticePublishType;
import io.rosecloud.notice.domain.NoticeRecord;
import io.rosecloud.notice.domain.NoticeRepository;
import io.rosecloud.notice.domain.NoticeStatus;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.notice.error.NoticeErrorCode;
import io.rosecloud.notice.service.NoticeService;
import io.rosecloud.notice.service.dto.MyNotice;
import io.rosecloud.notice.channel.NoticeDispatchService;
import io.rosecloud.starter.audit.AuditLog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeDispatchService dispatchService;

    public NoticeServiceImpl(NoticeRepository noticeRepository, NoticeDispatchService dispatchService) {
        this.noticeRepository = noticeRepository;
        this.dispatchService = dispatchService;
    }

    @AuditLog(action = "notice-publish", description = "发布通知")
    @Override
    public Long publish(NoticePublishRequest request) {
        validateTarget(request);
        LocalDateTime now = LocalDateTime.now();
        int publishType = request.publishType() == null ? NoticePublishType.IMMEDIATE.code() : request.publishType();
        int status;
        LocalDateTime publishTime;
        if (publishType == NoticePublishType.SCHEDULED.code()) {
            if (request.publishTime() == null || !request.publishTime().isAfter(now)) {
                throw new BizException(NoticeErrorCode.NOTICE_NOT_FOUND);
            }
            status = NoticeStatus.DRAFT.code();
            publishTime = request.publishTime();
        } else {
            status = NoticeStatus.PUBLISHED.code();
            publishTime = request.publishTime() != null ? request.publishTime() : now;
        }
        CurrentUser sender = UserContext.get();
        Long senderId = sender == null ? null : sender.userId();
        String senderTenantId = sender == null ? null : sender.tenantId();
        int channels = request.channels() == null ? NoticeChannel.defaultMask() : request.channels();
        Notice notice = new Notice(null, request.title(), request.content(), request.targetType(),
                request.targetTenantId(), request.targetRoleCode(), publishType, publishTime,
                request.effectiveTime(), request.expireTime(), status,
                Boolean.TRUE.equals(request.needConfirm()), senderId, senderTenantId, channels);
        Long id = noticeRepository.insert(notice);
        if (status == NoticeStatus.PUBLISHED.code()) {
            dispatchService.dispatch(notice.withId(id));
        }
        return id;
    }

    @Override
    public PageResult<Notice> page(long current, long size, String keyword) {
        return noticeRepository.page(current, size, keyword);
    }

    @Override
    public PageResult<MyNotice> myNotices(long current, long size) {
        CurrentUser user = UserContext.get();
        if (user == null || user.userId() == null) {
            return PageResult.empty(current, size);
        }
        LocalDateTime now = LocalDateTime.now();
        PageResult<Notice> notices = noticeRepository.myNotices(current, size, user.tenantId(), user.roles(), now);
        Map<Long, NoticeRecord> records = noticeRepository
                .findRecords(notices.records().stream().map(Notice::getId).toList(), user.userId())
                .stream().collect(Collectors.toMap(NoticeRecord::getNoticeId, Function.identity()));
        List<MyNotice> mine = notices.records().stream()
                .map(n -> toMyNotice(n, records.get(n.getId())))
                .toList();
        return PageResult.of(mine, notices.total(), notices.current(), notices.size());
    }

    @Override
    public MyNotice getMine(Long id) {
        Notice notice = loadAndCheckVisible(id);
        CurrentUser user = UserContext.get();
        NoticeRecord record = user == null ? null
                : noticeRepository.findRecords(List.of(id), user.userId()).stream().findFirst().orElse(null);
        return toMyNotice(notice, record);
    }

    @Override
    public void markRead(Long id) {
        Notice notice = loadAndCheckVisible(id);
        CurrentUser user = UserContext.get();
        if (user == null || user.userId() == null) {
            return;
        }
        noticeRepository.upsertRead(id, user.userId(), user.tenantId(), LocalDateTime.now());
    }

    @Override
    public void confirm(Long id) {
        Notice notice = loadAndCheckVisible(id);
        if (!Boolean.TRUE.equals(notice.getNeedConfirm())) {
            throw new BizException(NoticeErrorCode.NOTICE_NOT_CONFIRMABLE);
        }
        CurrentUser user = UserContext.get();
        if (user == null || user.userId() == null) {
            return;
        }
        noticeRepository.upsertConfirm(id, user.userId(), user.tenantId(), LocalDateTime.now());
    }

    @Override
    public int publishScheduledNotices() {
        LocalDateTime now = LocalDateTime.now();
        List<Notice> due = noticeRepository.findDueScheduled(now);
        for (Notice notice : due) {
            noticeRepository.markPublished(notice.getId());
            dispatchService.dispatch(notice);
        }
        return due.size();
    }

    private Notice loadAndCheckVisible(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new BizException(NoticeErrorCode.NOTICE_NOT_FOUND));
        CurrentUser user = UserContext.get();
        if (!visibleTo(notice, user, LocalDateTime.now())) {
            throw new BizException(NoticeErrorCode.NOTICE_NOT_VISIBLE);
        }
        return notice;
    }

    private boolean visibleTo(Notice n, CurrentUser user, LocalDateTime now) {
        if (n.getStatus() == null || n.getStatus() != NoticeStatus.PUBLISHED.code()) {
            return false;
        }
        if (n.getPublishTime() != null && n.getPublishTime().isAfter(now)) {
            return false;
        }
        if (n.getEffectiveTime() != null && n.getEffectiveTime().isAfter(now)) {
            return false;
        }
        if (n.getExpireTime() != null && n.getExpireTime().isBefore(now)) {
            return false;
        }
        int type = n.getTargetType() == null ? -1 : n.getTargetType();
        if (type == NoticeTargetType.GLOBAL.code()) {
            return true;
        }
        if (type == NoticeTargetType.TENANT.code()) {
            return user != null && user.tenantId() != null && user.tenantId().equals(n.getTargetTenantId());
        }
        if (type == NoticeTargetType.ROLE.code()) {
            return user != null && n.getTargetRoleCode() != null && user.roles() != null
                    && user.roles().contains(n.getTargetRoleCode());
        }
        return false;
    }

    private void validateTarget(NoticePublishRequest request) {
        int type = request.targetType() == null ? -1 : request.targetType();
        if (type == NoticeTargetType.TENANT.code() && request.targetTenantId() == null) {
            throw new BizException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
        if (type == NoticeTargetType.ROLE.code() && (request.targetRoleCode() == null || request.targetRoleCode().isBlank())) {
            throw new BizException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
    }

    private MyNotice toMyNotice(Notice notice, NoticeRecord record) {
        boolean read = record != null && record.getReadTime() != null;
        boolean confirmed = record != null && record.getConfirmTime() != null;
        return new MyNotice(notice, read, confirmed,
                record == null ? null : record.getReadTime(),
                record == null ? null : record.getConfirmTime());
    }
}
