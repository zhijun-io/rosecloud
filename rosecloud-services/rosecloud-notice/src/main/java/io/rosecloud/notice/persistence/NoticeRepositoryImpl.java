package io.rosecloud.notice.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.rosecloud.common.core.model.PageResult;
import io.rosecloud.notice.domain.Notice;
import io.rosecloud.notice.domain.NoticePublishType;
import io.rosecloud.notice.domain.NoticeRecord;
import io.rosecloud.notice.domain.NoticeRepository;
import io.rosecloud.notice.domain.NoticeStatus;
import io.rosecloud.notice.domain.NoticeTargetType;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class NoticeRepositoryImpl implements NoticeRepository {

    private final NoticeMapper noticeMapper;
    private final NoticeRecordMapper recordMapper;

    public NoticeRepositoryImpl(NoticeMapper noticeMapper, NoticeRecordMapper recordMapper) {
        this.noticeMapper = noticeMapper;
        this.recordMapper = recordMapper;
    }

    @Override
    public Long insert(Notice notice) {
        NoticePO po = toPO(notice);
        po.setId(null);
        noticeMapper.insert(po);
        return po.getId();
    }

    @Override
    public int publishScheduled(LocalDateTime now) {
        return noticeMapper.update(null, new LambdaUpdateWrapper<NoticePO>()
                .eq(NoticePO::getStatus, NoticeStatus.DRAFT.code())
                .eq(NoticePO::getPublishType, NoticePublishType.SCHEDULED.code())
                .le(NoticePO::getPublishTime, now)
                .set(NoticePO::getStatus, NoticeStatus.PUBLISHED.code()));
    }

    @Override
    public PageResult<Notice> page(long current, long size, String keyword) {
        Page<NoticePO> page = new Page<>(current, size);
        LambdaQueryWrapper<NoticePO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(NoticePO::getTitle, keyword);
        }
        wrapper.orderByDesc(NoticePO::getCreateTime);
        IPage<NoticePO> result = noticeMapper.selectPage(page, wrapper);
        List<Notice> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public Optional<Notice> findById(Long id) {
        return Optional.ofNullable(noticeMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<Notice> myNotices(long current, long size, Long tenantId,
                                        Collection<String> roleCodes, LocalDateTime now) {
        Page<NoticePO> page = new Page<>(current, size);
        LambdaQueryWrapper<NoticePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticePO::getStatus, NoticeStatus.PUBLISHED.code());
        wrapper.le(NoticePO::getPublishTime, now);
        wrapper.and(q -> q.isNull(NoticePO::getEffectiveTime).or().le(NoticePO::getEffectiveTime, now));
        wrapper.and(q -> q.isNull(NoticePO::getExpireTime).or().ge(NoticePO::getExpireTime, now));
        wrapper.and(q -> {
            q.eq(NoticePO::getTargetType, NoticeTargetType.GLOBAL.code());
            if (tenantId != null) {
                q.or().and(t -> t.eq(NoticePO::getTargetType, NoticeTargetType.TENANT.code())
                        .eq(NoticePO::getTargetTenantId, tenantId));
            }
            if (roleCodes != null && !roleCodes.isEmpty()) {
                q.or().and(t -> t.eq(NoticePO::getTargetType, NoticeTargetType.ROLE.code())
                        .in(NoticePO::getTargetRoleCode, roleCodes));
            }
        });
        wrapper.orderByDesc(NoticePO::getPublishTime);
        IPage<NoticePO> result = noticeMapper.selectPage(page, wrapper);
        List<Notice> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<NoticeRecord> findRecords(Collection<Long> noticeIds, Long userId) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return List.of();
        }
        return recordMapper.selectList(new LambdaQueryWrapper<NoticeRecordPO>()
                        .in(NoticeRecordPO::getNoticeId, noticeIds)
                        .eq(NoticeRecordPO::getUserId, userId))
                .stream().map(this::toRecordDomain).toList();
    }

    @Override
    public void upsertRead(Long noticeId, Long userId, Long tenantId, LocalDateTime now) {
        NoticeRecordPO existing = recordMapper.selectOne(new LambdaQueryWrapper<NoticeRecordPO>()
                .eq(NoticeRecordPO::getNoticeId, noticeId)
                .eq(NoticeRecordPO::getUserId, userId));
        if (existing == null) {
            NoticeRecordPO po = new NoticeRecordPO();
            po.setNoticeId(noticeId);
            po.setUserId(userId);
            po.setTenantId(tenantId);
            po.setReadTime(now);
            recordMapper.insert(po);
        } else if (existing.getReadTime() == null) {
            recordMapper.update(null, new LambdaUpdateWrapper<NoticeRecordPO>()
                    .eq(NoticeRecordPO::getId, existing.getId())
                    .set(NoticeRecordPO::getReadTime, now));
        }
    }

    @Override
    public void upsertConfirm(Long noticeId, Long userId, Long tenantId, LocalDateTime now) {
        NoticeRecordPO existing = recordMapper.selectOne(new LambdaQueryWrapper<NoticeRecordPO>()
                .eq(NoticeRecordPO::getNoticeId, noticeId)
                .eq(NoticeRecordPO::getUserId, userId));
        if (existing == null) {
            NoticeRecordPO po = new NoticeRecordPO();
            po.setNoticeId(noticeId);
            po.setUserId(userId);
            po.setTenantId(tenantId);
            po.setReadTime(now);
            po.setConfirmTime(now);
            recordMapper.insert(po);
        } else {
            LambdaUpdateWrapper<NoticeRecordPO> update = new LambdaUpdateWrapper<NoticeRecordPO>()
                    .eq(NoticeRecordPO::getId, existing.getId())
                    .set(NoticeRecordPO::getConfirmTime, now);
            if (existing.getReadTime() == null) {
                update.set(NoticeRecordPO::getReadTime, now);
            }
            recordMapper.update(null, update);
        }
    }

    private Notice toDomain(NoticePO po) {
        return new Notice(po.getId(), po.getTitle(), po.getContent(), po.getTargetType(),
                po.getTargetTenantId(), po.getTargetRoleCode(), po.getPublishType(), po.getPublishTime(),
                po.getEffectiveTime(), po.getExpireTime(), po.getStatus(), po.getNeedConfirm(),
                po.getSenderId(), po.getTenantId());
    }

    private NoticeRecord toRecordDomain(NoticeRecordPO po) {
        return new NoticeRecord(po.getId(), po.getNoticeId(), po.getUserId(), po.getTenantId(),
                po.getReadTime(), po.getConfirmTime());
    }

    private NoticePO toPO(Notice n) {
        NoticePO po = new NoticePO();
        po.setId(n.id());
        po.setTitle(n.title());
        po.setContent(n.content());
        po.setTargetType(n.targetType());
        po.setTargetTenantId(n.targetTenantId());
        po.setTargetRoleCode(n.targetRoleCode());
        po.setPublishType(n.publishType());
        po.setPublishTime(n.publishTime());
        po.setEffectiveTime(n.effectiveTime());
        po.setExpireTime(n.expireTime());
        po.setStatus(n.status());
        po.setNeedConfirm(n.needConfirm());
        po.setSenderId(n.senderId());
        po.setTenantId(n.tenantId());
        return po;
    }
}
