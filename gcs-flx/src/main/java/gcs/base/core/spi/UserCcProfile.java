package gcs.base.core.spi;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户保存时随表单提交的呼叫中心档案字段（工号 / 工号密码 / 选线策略 / 指定外线）。
 *
 * <p>用「键是否出现」而不是「值是否为空」来判断要不要写库：用户更新是部分提交，前端只发当前
 * 表单里存在的字段。非呼叫中心环境不渲染这些字段，若把「没出现」当成「要清空」，保存一次用户
 * 就会把已有工号抹掉。因此这里只承担「摘出出现过的键」，动哪几列由写入方决定。</p>
 *
 * <p>契约放在 {@code gcs.base} 而不是呼叫中心模块：ugrp 用软查找
 * （{@code GcsHolder.getBeansOfType}）找写入方，但不必知道谁实现，呼叫中心被裁掉时 ugrp 照常工作。</p>
 */
public class UserCcProfile {

    /** 工号（{@code cc_sip_user.cc_job_num}） */
    public static final String CC_JOB_NUM = "ccJobNum";
    /** 工号密码（{@code cc_sip_user.cc_job_pwd}），话机「工号 + 密码」登录用 */
    public static final String CC_JOB_PWD = "ccJobPwd";
    /** 选线策略（{@code cc_sip_user.gateway_strategy}），覆盖所在分机的设置 */
    public static final String GATEWAY_STRATEGY = "gatewayStrategy";
    /** 指定外线（{@code cc_sip_user.strategy_pattern}），覆盖所在分机的设置 */
    public static final String STRATEGY_PATTERN = "strategyPattern";

    private static final String[] FIELDS = {CC_JOB_NUM, CC_JOB_PWD, GATEWAY_STRATEGY, STRATEGY_PATTERN};

    private final Map<String, String> values = new LinkedHashMap<>();

    /** 摘出表单里出现过的 cc* 字段；空白串归一为 null，表示「清空该列」。 */
    public static UserCcProfile fromJsonMap(Map<String, Object> jsonMap) {
        UserCcProfile profile = new UserCcProfile();
        if (jsonMap == null) {
            return profile;
        }
        for (String field : FIELDS) {
            if (jsonMap.containsKey(field)) {
                profile.values.put(field, normalize(jsonMap.get(field)));
            }
        }
        return profile;
    }

    private static String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /** 表单里没有任何 cc* 字段：本次保存与呼叫中心无关。 */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * 展示用的空字段集合：四个键恒出现、值为 null（该用户没有工号档案行）。
     *
     * <p>回显必须「键恒出现」：用户编辑弹窗会把这四个字段连同其余字段一起提交，
     * 有档案时不给值就会在保存时清空该档案。</p>
     */
    public static Map<String, Object> emptyDisplayFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (String field : FIELDS) {
            fields.put(field, null);
        }
        return fields;
    }

    /** 该字段是否出现在本次提交里（出现即表示要写，值为 null 表示清空）。 */
    public boolean has(String field) {
        return values.containsKey(field);
    }

    /** 字段值；未出现时返回 null，出现但被清空时同样返回 null（用 {@link #has} 区分）。 */
    public String get(String field) {
        return values.get(field);
    }
}
