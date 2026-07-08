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
import io.rosecloud.api.notice.NoticeTargetType;
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
        NoticeEntity entity = toEntity(notice);
        entity.setId(null);
        noticeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public List<Notice> findDueScheduled(LocalDateTime now) {
        return noticeMapper.selectList(new LambdaQueryWrapper<NoticeEntity>()
                .eq(NoticeEntity::getStatus, NoticeStatus.DRAFT.code())
                .eq(NoticeEntity::getPublishType, NoticePublishType.SCHEDULED.code())
                .le(NoticeEntity::getPublishTime, now))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void markPublished(Long id) {
        noticeMapper.update(null, new LambdaUpdateWrapper<NoticeEntity>()
                .eq(NoticeEntity::getId, id)
                .set(NoticeEntity::getStatus, NoticeStatus.PUBLISHED.code()));
    }

    @Override
    public PageResult<Notice> page(long current, long size, String keyword) {
        Page<NoticeEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(NoticeEntity::getTitle, keyword);
        }
        wrapper.orderByDesc(NoticeEntity::getCreateTime);
        IPage<NoticeEntity> result = noticeMapper.selectPage(page, wrapper);
        List<Notice> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public Optional<Notice> findById(Long id) {
        return Optional.ofNullable(noticeMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public PageResult<Notice> myNotices(long current, long size, String tenantId,
                                        Collection<String> roleCodes, String username, LocalDateTime now) {
        Page<NoticeEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<NoticeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeEntity::getStatus, NoticeStatus.PUBLISHED.code());
        wrapper.le(NoticeEntity::getPublishTime, now);
        wrapper.and(q -> q.isNull(NoticeEntity::getEffectiveTime).or().le(NoticeEntity::getEffectiveTime, now));
        wrapper.and(q -> q.isNull(NoticeEntity::getExpireTime).or().ge(NoticeEntity::getExpireTime, now));
        wrapper.and(q -> {
            q.eq(NoticeEntity::getTargetType, NoticeTargetType.GLOBAL.code());
            if (tenantId != null) {
                q.or().and(t -> t.eq(NoticeEntity::getTargetType, NoticeTargetType.TENANT.code())
                        .eq(NoticeEntity::getTargetTenantId, tenantId));
            }
            if (roleCodes != null && !roleCodes.isEmpty()) {
                q.or().and(t -> t.eq(NoticeEntity::getTargetType, NoticeTargetType.ROLE.code())
                        .in(NoticeEntity::getTargetRoleCode, roleCodes));
            }
            if (username != null && !username.isBlank()) {
                q.or().and(t -> t.eq(NoticeEntity::getTargetType, NoticeTargetType.USER.code())
                        .eq(NoticeEntity::getTargetUsername, username));
            }
        });
        wrapper.orderByDesc(NoticeEntity::getPublishTime);
        IPage<NoticeEntity> result = noticeMapper.selectPage(page, wrapper);
        List<Notice> records = result.getRecords().stream().map(this::toDomain).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<NoticeRecord> findRecords(Collection<Long> noticeIds, Long userId) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return List.of();
        }
        return recordMapper.selectList(new LambdaQueryWrapper<NoticeRecordEntity>()
                        .in(NoticeRecordEntity::getNoticeId, noticeIds)
                        .eq(NoticeRecordEntity::getUserId, userId))
                .stream().map(this::toRecordDomain).toList();
    }

    @Override
    public void upsertRead(Long noticeId, Long userId, String tenantId, LocalDateTime now) {
        NoticeRecordEntity existing = recordMapper.selectOne(new LambdaQueryWrapper<NoticeRecordEntity>()
                .eq(NoticeRecordEntity::getNoticeId, noticeId)
                .eq(NoticeRecordEntity::getUserId, userId));
        if (existing == null) {
            NoticeRecordEntity entity = new NoticeRecordEntity();
            entity.setNoticeId(noticeId);
            entity.setUserId(userId);
            entity.setTenantId(tenantId);
            entity.setReadTime(now);
            recordMapper.insert(entity);
        } else if (existing.getReadTime() == null) {
            recordMapper.update(null, new LambdaUpdateWrapper<NoticeRecordEntity>()
                    .eq(NoticeRecordEntity::getId, existing.getId())
                    .set(NoticeRecordEntity::getReadTime, now));
        }
    }

    @Override
    public void upsertConfirm(Long noticeId, Long userId, String tenantId, LocalDateTime now) {
        NoticeRecordEntity existing = recordMapper.selectOne(new LambdaQueryWrapper<NoticeRecordEntity>()
                .eq(NoticeRecordEntity::getNoticeId, noticeId)
                .eq(NoticeRecordEntity::getUserId, userId));
        if (existing == null) {
            NoticeRecordEntity entity = new NoticeRecordEntity();
            entity.setNoticeId(noticeId);
            entity.setUserId(userId);
            entity.setTenantId(tenantId);
            entity.setReadTime(now);
            entity.setConfirmTime(now);
            recordMapper.insert(entity);
        } else {
            LambdaUpdateWrapper<NoticeRecordEntity> update = new LambdaUpdateWrapper<NoticeRecordEntity>()
                    .eq(NoticeRecordEntity::getId, existing.getId())
                    .set(NoticeRecordEntity::getConfirmTime, now);
            if (existing.getReadTime() == null) {
                update.set(NoticeRecordEntity::getReadTime, now);
            }
            recordMapper.update(null, update);
        }
    }

    private Notice toDomain(NoticeEntity entity) {
        return new Notice(entity.getId(), entity.getTitle(), entity.getContent(), entity.getTargetType(),
                entity.getTargetTenantId(), entity.getTargetRoleCode(), entity.getTargetUsername(), entity.getPublishType(), entity.getPublishTime(),
                entity.getEffectiveTime(), entity.getExpireTime(), entity.getStatus(), entity.getNeedConfirm(),
                entity.getSenderId(), entity.getTenantId(), entity.getChannels());
    }

    private NoticeRecord toRecordDomain(NoticeRecordEntity entity) {
        return new NoticeRecord(entity.getId(), entity.getNoticeId(), entity.getUserId(), entity.getTenantId(),
                entity.getReadTime(), entity.getConfirmTime());
    }

    private NoticeEntity toEntity(Notice n) {
        NoticeEntity entity = new NoticeEntity();
        entity.setId(n.getId());
        entity.setTitle(n.getTitle());
        entity.setContent(n.getContent());
        entity.setTargetType(n.getTargetType());
        entity.setTargetTenantId(n.getTargetTenantId());
        entity.setTargetRoleCode(n.getTargetRoleCode());
        entity.setTargetUsername(n.getTargetUsername());
        entity.setPublishType(n.getPublishType());
        entity.setPublishTime(n.getPublishTime());
        entity.setEffectiveTime(n.getEffectiveTime());
        entity.setExpireTime(n.getExpireTime());
        entity.setStatus(n.getStatus());
        entity.setNeedConfirm(n.getNeedConfirm());
        entity.setSenderId(n.getSenderId());
        entity.setTenantId(n.getTenantId());
        entity.setChannels(n.getChannels());
        return entity;
    }
}
