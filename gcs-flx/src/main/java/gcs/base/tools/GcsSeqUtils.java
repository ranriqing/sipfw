package gcs.base.tools;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * 使用示例：
 * GcsSeqUtils.nextGeneralSerial("sno_haibo", "CHN%2$05d"); 忽略日期
 * GcsSeqUtils.nextDailySerial("sno_haibo", "%1$sCHN%2$05d", "yyyy") 指定日期格式
 * GcsSeqUtils.nextDailySerial("sno_haibo", "{yy}CHN%2$05d") 内联日期格式（等价 %1$sCHN%2$05d + "yy"）
 */
public class GcsSeqUtils {

    /** 普通递增序列号 */
    public static String nextGeneralSerial(String snoSeqName){
        return nextGeneralSerial(snoSeqName, "%1$sCHN%2$05d");
    }

    public static String nextGeneralSerial(String snoSeqName, String serialFormat){
        return nextGeneralSerial(snoSeqName, serialFormat, "yyyyMMdd");
    }

    public static String nextGeneralSerial(String snoSeqName, String serialFormat, String dateFormat){
        String[] resolved = resolveFormat(serialFormat, dateFormat);
        String dateStr = DateUtil.format(new Date(), resolved[1]);
        Integer seqNum = (Integer) SqlRunner.db().selectObj("select get_general_sequence('" + snoSeqName + "')");
        return String.format(resolved[0], dateStr, seqNum);
    }

    /** 按年重置序列号 */
    public static String nextYearlySerial(String snoSeqName){
        return nextYearlySerial(snoSeqName, "%1$sCHN%2$05d");
    }
    public static String nextYearlySerial(String snoSeqName, String serialFormat){
        return nextYearlySerial(snoSeqName, serialFormat, "yy");
    }
    public static String nextYearlySerial(String snoSeqName, String serialFormat, String dateFormat){
        String[] resolved = resolveFormat(serialFormat, dateFormat);
        String dateStr = DateUtil.format(new Date(), resolved[1]);
        Integer seqNum = (Integer) SqlRunner.db().selectObj("select get_yearly_sequence('" + snoSeqName + "')");
        return String.format(resolved[0], dateStr, seqNum);
    }

    /** 按天重置序列号 */
    public static String nextDailySerial(String snoSeqName){
        return nextDailySerial(snoSeqName, "%1$sCHN%2$05d");
    }
    public static String nextDailySerial(String snoSeqName, String serialFormat){
        return nextDailySerial(snoSeqName, serialFormat, "yyyyMMdd");
    }
    public static String nextDailySerial(String snoSeqName, String serialFormat, String dateFormat){
        String[] resolved = resolveFormat(serialFormat, dateFormat);
        String dateStr = DateUtil.format(new Date(), resolved[1]);
        Integer seqNum = (Integer) SqlRunner.db().selectObj("select get_daily_sequence('" + snoSeqName + "')");
        return String.format(resolved[0], dateStr, seqNum);
    }

    /**
     * 解析序列格式：若 serialFormat 内含 {日期格式}（如 {yy}），则提取为日期格式，并将 {…} 替换为日期占位符 %1$s。
     * 返回 [解析后的 serialFormat, 日期格式]。
     */
    private static String[] resolveFormat(String serialFormat, String dateFormat){
        String pattern = "\\{([^{}]+)\\}";
        String fmt = ReUtil.get(pattern, serialFormat, 1);
        if (fmt != null) {
            dateFormat = fmt;
            serialFormat = ReUtil.replaceAll(serialFormat, pattern, "%1$s");
        }
        return new String[]{ serialFormat, dateFormat };
    }
}
