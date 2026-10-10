package gcs.base.core.spi;

/**
 * 用户（{@code gcs_user}）保存后，把随表单提交的呼叫中心档案字段落到 {@code cc_sip_user}。
 *
 * <p>由呼叫中心模块实现并通过 Spring 注册为 Bean；ugrp 侧用
 * {@code GcsHolder.getBeansOfType(UserCcProfileWriter.class)} 软查找调用：</p>
 * <ul>
 *   <li>取不到 Bean（呼叫中心被裁掉或未启用）→ 调用方静默跳过，用户照常保存；</li>
 *   <li>取到但校验失败（工号已被他人占用）→ 抛 {@code GcsException}，与用户保存同一事务回滚
 *       —— 工号唯一性必须对管理员可见，不能吞。</li>
 * </ul>
 */
public interface UserCcProfileWriter {

    /**
     * 写入/清空该用户的呼叫中心档案。
     *
     * @param userId  用户 id（{@code cc_sip_user.userid}）
     * @param profile 本次表单里出现过的字段，缺省字段表示「不涉及，保持原值」
     */
    void write(Long userId, UserCcProfile profile);
}
