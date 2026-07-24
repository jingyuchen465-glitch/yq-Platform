package com.itcjy.common.constants;

public class ClassScheduleConstants {

    private ClassScheduleConstants() {
    }

    /** 数据库中的排课规则配置类型编码。 */
    public static final String CONFIG_TYPE_CODE = "CLASS_SCHEDULE_RULE";

    /**
     * 课表中每天可能的类型
     */
    public static final class DayType {
        private DayType() {
        }

        /** 正常上课，绑定课程详情 */
        public static final String CLASS = "CLASS";
        /** 自习日，不消耗课程详情 */
        public static final String SELF_STUDY = "SELF_STUDY";
        /** 固定休息日，不消耗课程详情 */
        public static final String REST = "REST";
        /** 法定节假日，不消耗课程详情 */
        public static final String HOLIDAY = "HOLIDAY";
    }

    /** 排课规则数据库配置项键。 */
    public static final class RuleKey {
        private RuleKey() {
        }

        /** 上课日（星期几集合，1=周一 ... 7=周日） */
        public static final String CLASS_DAYS = "CLASSDAYS";
        /** 自习日（星期几集合） */
        public static final String SELF_STUDY_DAYS = "SELFSTUDYDAYS";
        /** 固定休息日（星期几集合） */
        public static final String REST_DAYS = "RESTDAYS";
        /** 法定节假日是否自动休息 */
        public static final String HOLIDAY_REST = "HOLIDAYREST";
    }
}
