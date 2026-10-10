package gcs.base.core.spi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UserCcProfile} 的「键是否出现」语义测试。
 *
 * <p>守的是一条静默数据损坏的边界：用户更新走的是<b>部分提交</b>（前端只发当前表单里的字段），
 * 所以「键没出现」与「键出现但为空」必须区分开 ——
 * 前者表示"本次不涉及该列"，后者表示"管理员把它清空了"。
 * 若不区分，非呼叫中心表单（不渲染 cc* 字段）保存用户时会把已有工号整列清掉。</p>
 */
class UserCcProfileTest {

    @Test
    @DisplayName("表单没有 cc* 键时不产生任何待写字段")
    void noCcKeysMeansEmpty() {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("username", "zhangsan");
        jsonMap.put("nickname", "张三");

        UserCcProfile profile = UserCcProfile.fromJsonMap(jsonMap);

        assertTrue(profile.isEmpty());
        assertFalse(profile.has(UserCcProfile.CC_JOB_NUM));
        assertFalse(profile.has(UserCcProfile.CC_JOB_PWD));
        assertFalse(profile.has(UserCcProfile.GATEWAY_STRATEGY));
        assertFalse(profile.has(UserCcProfile.STRATEGY_PATTERN));
    }

    @Test
    @DisplayName("键出现即算待写，即使值为 null（表示清空该列）")
    void presentKeyWithNullValueIsStillPresent() {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put(UserCcProfile.CC_JOB_NUM, null);

        UserCcProfile profile = UserCcProfile.fromJsonMap(jsonMap);

        assertFalse(profile.isEmpty());
        assertTrue(profile.has(UserCcProfile.CC_JOB_NUM));
        assertNull(profile.get(UserCcProfile.CC_JOB_NUM));
    }

    @Test
    @DisplayName("空白串归一为 null（前端清空输入框提交空串）")
    void blankValueNormalizedToNull() {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put(UserCcProfile.CC_JOB_NUM, "   ");

        UserCcProfile profile = UserCcProfile.fromJsonMap(jsonMap);

        assertTrue(profile.has(UserCcProfile.CC_JOB_NUM));
        assertNull(profile.get(UserCcProfile.CC_JOB_NUM));
    }

    @Test
    @DisplayName("数字型工号转成字符串")
    void numericValueConvertedToString() {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put(UserCcProfile.CC_JOB_NUM, 1001);

        UserCcProfile profile = UserCcProfile.fromJsonMap(jsonMap);

        assertEquals("1001", profile.get(UserCcProfile.CC_JOB_NUM));
    }

    @Test
    @DisplayName("只收录 cc* 键，其余键（含相似名）不进入待写集合")
    void onlyCcKeysAreCollected() {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put(UserCcProfile.CC_JOB_NUM, "1001");
        jsonMap.put("ccJobNumX", "1002");
        jsonMap.put("remark", "备注");

        UserCcProfile profile = UserCcProfile.fromJsonMap(jsonMap);

        assertTrue(profile.has(UserCcProfile.CC_JOB_NUM));
        assertFalse(profile.has("ccJobNumX"));
        assertFalse(profile.has("remark"));
        assertEquals("1001", profile.get(UserCcProfile.CC_JOB_NUM));
    }

    @Test
    @DisplayName("null 表单不抛异常，视为空")
    void nullJsonMapIsEmpty() {
        assertTrue(UserCcProfile.fromJsonMap(null).isEmpty());
    }

    @Test
    @DisplayName("空展示字段：四个键恒出现且值为 null（无档案行的用户）")
    void emptyDisplayFieldsHasAllKeysWithNullValues() {
        Map<String, Object> fields = UserCcProfile.emptyDisplayFields();

        assertEquals(4, fields.size());
        for (String field : new String[]{
                UserCcProfile.CC_JOB_NUM,
                UserCcProfile.CC_JOB_PWD,
                UserCcProfile.GATEWAY_STRATEGY,
                UserCcProfile.STRATEGY_PATTERN}) {
            assertTrue(fields.containsKey(field), "缺少键：" + field);
            assertNull(fields.get(field));
        }
    }
}
