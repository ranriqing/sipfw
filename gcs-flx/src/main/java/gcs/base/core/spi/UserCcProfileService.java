package gcs.base.core.spi;

import java.util.Collection;
import java.util.Map;

/**
 * 用户（{@code gcs_user}）与呼叫中心档案（{@code cc_sip_user}）之间的读写契约。
 *
 * <p>由呼叫中心模块实现并注册为 Spring Bean；ugrp 侧用
 * {@code GcsHolder.getBeansOfType(UserCcProfileService.class)} 软查找调用，因此 ugrp
 * 不依赖呼叫中心，呼叫中心被裁掉时用户模块照常工作。</p>
 *
 * <p>容错规则（两侧语义不同，别混）：</p>
 * <ul>
 *   <li>取不到 Bean（呼叫中心未启用）→ 调用方静默跳过：写入不报错，展示字段留空。</li>
 *   <li>取到但校验失败（工号被他人占用）→ {@link #write} 抛 {@code GcsException}，与用户保存
 *       同一事务回滚。工号唯一性必须对管理员可见，不能吞。</li>
 * </ul>
 */
public interface UserCcProfileService {

    /**
     * 写入/清空该用户的呼叫中心档案字段。
     *
     * @param userId  用户 id（{@code cc_sip_user.userid}）
     * @param profile 本次表单里出现过的字段；未出现的字段保持原值（不涉及）
     */
    void write(Long userId, UserCcProfile profile);

    /**
     * 展示用字段：{@code ccJobNum / ccJobPwd / gatewayStrategy / strategyPattern} 各键恒出现，
     * 无档案行时值为 null。
     *
     * <p>必须先回显再允许编辑：用户编辑弹窗会把这四个字段连同其余字段一起提交，若这里不给值，
     * 表单里就是空的，保存一次就会把已有工号与工号密码清掉。</p>
     *
     * @param userIds 用户 id 集合（列表页一次批量取，避免逐行查询）
     * @return userId → 字段 Map；无档案行的用户不会出现在结果里
     */
    Map<Long, Map<String, Object>> displayFields(Collection<Long> userIds);
}
