package gcs.base.tools;

import gcs.base.core.GcsConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class GcsDateTimeUtils {

    /**
     * 日期周
     * @param date
     * @return
     */
    public static String weekDateStr(Date date) {
        if (date == null) {
            return "";
        }
        List<String> weekDays = new ArrayList<String>();
        weekDays.add("周日");
        weekDays.add("周一");
        weekDays.add("周二");
        weekDays.add("周三");
        weekDays.add("周四");
        weekDays.add("周五");
        weekDays.add("周六");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int weekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        weekIndex = weekIndex < 0 ? 0 : weekIndex;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        return sdf.format(date) + " " + weekDays.get(weekIndex);
    }

    /**
     * 时间美化
     * @return
     */
    public static String prettyTime(Date date) {
        if (date == null) {
            return "";
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        long time = date.getTime();

        if (isThisYear(date)) {                     // 今年
            if (isToday(date)) {                    // 今天
                int minute = minutesAgo(time);
                if (minute < 60) {                  // 1小时之内
                    return minute <= 1 ? "刚刚" : minute + "分钟前";
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    return sdf.format(date);
                }
            } else {
                if (isYesterday(date)) {            // 昨天
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    return "昨天 " + sdf.format(date);
                } else if (isThisWeek(date)) {      // 本周
                    List<String> weekDays = new ArrayList<String>();
                    weekDays.add("周日");
                    weekDays.add("周一");
                    weekDays.add("周二");
                    weekDays.add("周三");
                    weekDays.add("周四");
                    weekDays.add("周五");
                    weekDays.add("周六");

                    int weekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                    weekIndex = weekIndex < 0 ? 0 : weekIndex;

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    return weekDays.get(weekIndex) + " " + sdf.format(date);
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
                    return sdf.format(date);
                }
            }
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat(GcsConstants.DATETIME_MIN_PATTERN);
            return sdf.format(date);
        }
    }

    private static int minutesAgo(long time) {
        return (int) ((System.currentTimeMillis() - time) / (60000));
    }

    private static boolean isToday(Date date) {
        Date now = new Date();
        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTime(now);

        return (dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) &&
                (dateCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)) &&
                (dateCal.get(Calendar.DATE) == nowCal.get(Calendar.DATE));
    }

    private static boolean isYesterday(Date date) {
        Date now = new Date();
        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTime(now);

        return (dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) &&
                (dateCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)) &&
                (dateCal.get(Calendar.DATE) + 1 == nowCal.get(Calendar.DATE));
    }

    private static boolean isThisWeek(Date date) {
        Date now = new Date();
        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTime(now);

        if ((dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) &&
                (dateCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH))) {

            if (nowCal.get(Calendar.DAY_OF_WEEK) - dateCal.get(Calendar.DAY_OF_WEEK) < nowCal.get(Calendar.DAY_OF_WEEK)
                    && nowCal.get(Calendar.DATE) - dateCal.get(Calendar.DATE) > 0
                    && nowCal.get(Calendar.DATE) - dateCal.get(Calendar.DATE) < 7) {
                return true;
            }
        }
        return false;
    }

    private static boolean isThisYear(Date date) {
        Date now = new Date();
        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(date);
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTime(now);

        return dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR);
    }
}
