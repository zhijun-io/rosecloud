package io.rosecloud.notice.service.impl;

import io.rosecloud.api.notice.NoticePublishApi;
import io.rosecloud.api.notice.NoticePublishRequest;
import io.rosecloud.api.notice.NoticeTargetType;
import io.rosecloud.common.core.error.BizException;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.notice.channel.NoticeDispatchService;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticeChannel;
import io.rosecloud.notice.domain.NoticePublishType;
import io.rosecloud.notice.domain.NoticeRecord;
import io.rosecloud.notice.domain.NoticeRepository;
import io.rosecloud.notice.domain.NoticeStatus;
import io.rosecloud.notice.error.NoticeErrorCode;
import io.rosecloud.notice.service.NoticeService;
import io.rosecloud.notice.service.dto.MyNotice;
import io.rosecloud.starter.audit.AuditLog;
import io.rosecloud.common.security.model.SecurityUser;
import io.rosecloud.starter.tenant.core.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NoticeServiceImpl implements NoticeService, NoticePublishApi {

    private static final Logger log = LoggerFactory.getLogger(NoticeServiceImpl.class);

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
        SecurityUser sender = currentUser();
        Long senderId = sender == null ? null : sender.getUserId();
        String senderTenantId = TenantContext.getTenantId();
        int channels = request.channels() == null ? NoticeChannel.defaultMask() : request.channels();
        Notice notice = new Notice(null, request.title(), request.content(), request.targetType(),
                request.targetTenantId(), request.targetRoleCode(), request.targetUsername(), publishType, publishTime,
                request.effectiveTime(), request.expireTime(), status,
                Boolean.TRUE.equals(request.needConfirm()), senderId, senderTenantId, channels, request.recipients());
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
        SecurityUser user = currentUser();
        if (user == null || user.getUserId() == null) {
            return PageResult.empty(current, size);
        }
        LocalDateTime now = LocalDateTime.now();
        PageResult<Notice> notices = noticeRepository.myNotices(current, size, TenantContext.getTenantId(), roleNames(),
                user.getUsername(), now);
        Map<Long, NoticeRecord> records = noticeRepository
                .findRecords(notices.records().stream().map(Notice::getId).toList(), user.getUserId())
                .stream().collect(Collectors.toMap(NoticeRecord::getNoticeId, Function.identity()));
        List<MyNotice> mine = notices.records().stream()
                .map(n -> toMyNotice(n, records.get(n.getId())))
                .toList();
        return PageResult.of(mine, notices.total(), notices.current(), notices.size());
    }

    @Override
    public MyNotice getMine(Long id) {
        Notice notice = loadAndCheckVisible(id);
        SecurityUser user = currentUser();
        NoticeRecord record = user == null ? null
                : noticeRepository.findRecords(List.of(id), user.getUserId()).stream().findFirst().orElse(null);
        return toMyNotice(notice, record);
    }

    @Override
    public void markRead(Long id) {
        Notice notice = loadAndCheckVisible(id);
        SecurityUser user = currentUser();
        if (user == null || user.getUserId() == null) {
            return;
        }
        noticeRepository.upsertRead(id, user.getUserId(), TenantContext.getTenantId(), LocalDateTime.now());
    }

    @Override
    public void confirm(Long id) {
        Notice notice = loadAndCheckVisible(id);
        if (!Boolean.TRUE.equals(notice.getNeedConfirm())) {
            throw new BizException(NoticeErrorCode.NOTICE_NOT_CONFIRMABLE);
        }
        SecurityUser user = currentUser();
        if (user == null || user.getUserId() == null) {
            return;
        }
        noticeRepository.upsertConfirm(id, user.getUserId(), TenantContext.getTenantId(), LocalDateTime.now());
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

    @Scheduled(fixedDelayString = "${rosecloud.notice.publish-check-ms:60000}")
    public void publishDue() {
        int published = publishScheduledNotices();
        if (published > 0) {
            log.info("published {} scheduled notice(s)", published);
        }
    }

    private Notice loadAndCheckVisible(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new BizException(NoticeErrorCode.NOTICE_NOT_FOUND));
        if (!visibleTo(notice, currentUser(), LocalDateTime.now())) {
            throw new BizException(NoticeErrorCode.NOTICE_NOT_VISIBLE);
        }
        return notice;
    }

    private boolean visibleTo(Notice n, SecurityUser user, LocalDateTime now) {
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
            return user != null && TenantContext.getTenantId() != null && TenantContext.getTenantId().equals(n.getTargetTenantId());
        }
        if (type == NoticeTargetType.ROLE.code()) {
            return user != null && n.getTargetRoleCode() != null && !roleNames().isEmpty()
                    && roleNames().contains(n.getTargetRoleCode());
        }
        if (type == NoticeTargetType.USER.code()) {
            return user != null && n.getTargetUsername() != null && n.getTargetUsername().equals(user.getUsername());
        }
        return false;
    }

    private static SecurityUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser su)) {
            return null;
        }
        return su;
    }

    private static List<String> roleNames() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return List.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .toList();
    }

    private void validateTarget(NoticePublishRequest request) {
        int type = request.targetType() == null ? -1 : request.targetType();
        if (type == NoticeTargetType.TENANT.code() && request.targetTenantId() == null) {
            throw new BizException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
        if (type == NoticeTargetType.ROLE.code() && (request.targetRoleCode() == null || request.targetRoleCode().isBlank())) {
            throw new BizException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
        if (type == NoticeTargetType.USER.code() && (request.targetUsername() == null || request.targetUsername().isBlank())) {
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
