package gcs.base.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GcsNumberUtils {

    public static BigDecimal percentage(Number valueNum, Number totalNum) {
        return percentage(valueNum, totalNum, 4);
    }

    /**
     * 计算百分比（valueNum / totalNum）
     * @param valueNum 分子（需计算的数值）
     * @param totalNum 分母（总数）
     * @param scale 保留的小数位数（默认4位）
     * @return 百分比结果（范围：0~1）
     */
    public static BigDecimal percentage(Number valueNum, Number totalNum, Integer scale) {
        // 处理默认精度（如果scale为null，使用默认值4）
        int actualScale = (scale != null) ? scale : 4;

        BigDecimal result = BigDecimal.ZERO;

        // 校验输入：分子或分母为null时直接返回0
        if (valueNum == null || totalNum == null) {
            return result;
        }

        // 转换为BigDecimal以保证精度
        BigDecimal aValue = new BigDecimal(valueNum.toString());
        BigDecimal bValue = new BigDecimal(totalNum.toString());

        // 分母为0时返回0（避免除零异常）
        if (bValue.compareTo(BigDecimal.ZERO) == 0) {
            return result;
        }

        // 计算百分比（四舍五入）
        result = aValue.divide(bValue, actualScale, RoundingMode.HALF_UP);

        // 确保结果不超过1（如果分子大于分母，返回1）
        if (result.compareTo(BigDecimal.ONE) > 0) {
            result = BigDecimal.ONE;
        }

        return result;
    }

}