package io.dataease.plugins.datasource.sls.commons.utils;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class TimeRangeUtils {
    public static final String DATE_PATTERM = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";


    public static Date getDate(String dateString) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERM);
        return dateFormat.parse(dateString);
    }

    public static Date getTime(String timeString) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIME_PATTERN);
        return dateFormat.parse(timeString);
    }

    public static String getDateString(Date date) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERM);
        return dateFormat.format(date);
    }

    public static int getTimeByTimeRange(String type, Date time) throws Exception {
        int result = 0;
        if (StringUtils.equals(type, "oneMinutes")) {
            result = getOneMinutesTime(time);
        } else if (StringUtils.equals(type, "fiveMinutes")) {
            result = getFiveMinutesTime(time);
        } else if (StringUtils.equals(type, "oneHour")) {
            result = getOneHourTime(time);
        } else if (StringUtils.equals(type, "today")) {
            result = getTodayStartTime(time);
        } else if (StringUtils.equals(type, "oneDay")) {
            result = getOneDayTime(time);
        } else if (StringUtils.equals(type, "week")) {
            result = getCurrentWeekStartTime(time);
        } else if (StringUtils.equals(type, "month")) {
            result = getCurrentMonthStartTime(time);
        } else if (StringUtils.equals(type, "year")) {
            result = getCurrentYearStartTime(time);
        } else if (StringUtils.equals(type, "oneWeek")) {
            result = getOneWeekStartTime(time);
        } else if (StringUtils.equals(type, "oneMonth")) {
            result = getOneMonthStartTime(time);
        } else if (StringUtils.equals(type, "oneYear")) {
            result = getOneYearStartTime(time);
        } else {
            result = getTodayStartTime(time);
        }
        return result;
    }

    /**
     * 获取当前时间
     */
    public static int getCurrentTime(Date time) {
        return (int) (time.getTime() / 1000);
    }

    /**
     * 1分钟前：获取入参日期所在的时间 1 分钟前的时间
     */
    public static int getOneMinutesTime(Date time) {
        return (int) (time.getTime() / 1000) - 60;
    }

    /**
     * 5分钟前：获取入参日期所在的时间 5 分钟前的时间
     */
    public static int getFiveMinutesTime(Date time) {
        return (int) (time.getTime() / 1000) - 60 * 5;
    }

    /**
     * 1小时前：获取入参日期所在的时间 1 小时前的时间
     */
    public static int getOneHourTime(Date time) {
        return (int) (time.getTime() / 1000) - 3600;
    }

    /**
     * 1天前：获取入参日期所在的时间 24 小时前的时间
     */
    public static int getOneDayTime(Date time) {
        return (int) (time.getTime() / 1000) - 3600 * 24;
    }

    /**
     * 当天的起始时间
     */
    public static int getTodayStartTime(Date time) throws Exception {
        return (int) (getDate(getDateString(time)).getTime() / 1000);
    }

    /**
     * 7天前：获取入参日期所在的时间 1 周前的时间
     */
    public static int getOneWeekStartTime(Date time) {
        return (int) (time.getTime() / 1000) - 3600 * 24 * 7;
    }

    /**
     * 1月前：获取入参日期所在的时间 1 月前的时间
     */
    public static int getOneMonthStartTime(Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.MONTH, -1);
        Date date = cal.getTime();
        SimpleDateFormat format3= new SimpleDateFormat("yyyy-MM-dd");
        String dateStringYYYYMMDD3 = format3.format(date);
        return (int) (date.getTime() / 1000);
    }

    /**
     * 1年前：获取入参日期所在的时间 1 月前的时间
     */
    public static int getOneYearStartTime(Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        cal.add(Calendar.YEAR, -1);
        Date date = cal.getTime();
        SimpleDateFormat format3= new SimpleDateFormat("yyyy-MM-dd");
        String dateStringYYYYMMDD3 = format3.format(date);
        return (int) (date.getTime() / 1000);
    }


    /**
     * 本月周：本周起始时间
     */
    public static int getCurrentWeekStartTime(Date time) throws Exception {
        Calendar calendar = Calendar.getInstance();

        //Calendar默认一周的开始是周日。业务需求从周一开始算，所以要"+1"
        int weekDayAdd = 1;

        int thisWeekFirstTime = (int) (time.getTime() / 1000) - 3600 * 24 * 7;
        try {
            calendar.setTime(time);
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getActualMinimum(Calendar.DAY_OF_WEEK));
            calendar.add(Calendar.DAY_OF_MONTH, weekDayAdd);

            //第一天的时分秒是 00:00:00 这里直接取日期，默认就是零点零分
            Date thisWeekFirstDate = getDate(getDateString(calendar.getTime()));
            thisWeekFirstTime = (int) (getDate(getDateString(thisWeekFirstDate)).getTime() / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return thisWeekFirstTime;
    }

    /**
     * 本月：当月起始时间
     */
    public static int getCurrentMonthStartTime(Date time) throws Exception {
        Calendar calendar = Calendar.getInstance();

        int thisMonthFirstTime = (int) (time.getTime() / 1000) - 3600 * 24 * 7 * 4;
        try {
            calendar.setTime(time);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.add(Calendar.MONTH, 0);

            //第一天的时分秒是 00:00:00 这里直接取日期，默认就是零点零分
            Date thisMonthFirstDate = getDate(getDateString(calendar.getTime()));
            thisMonthFirstTime = (int) (getDate(getDateString(thisMonthFirstDate)).getTime() / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return thisMonthFirstTime;
    }

    /**
     * 本年：本年起始时间
     */
    public static int getCurrentYearStartTime(Date time) throws Exception {
        Calendar calendar = Calendar.getInstance();
        //获取当前年份
        int year = calendar.get(Calendar.YEAR);

        int thisYearFirstTime = (int) (time.getTime() / 1000) - 3600 * 24 * 365;
        try {
            calendar.clear();
            calendar.set(Calendar.YEAR, year);
            Date yearFirstDay = calendar.getTime();

            thisYearFirstTime = (int) (getDate(getDateString(yearFirstDay)).getTime() / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return thisYearFirstTime;
    }

}
