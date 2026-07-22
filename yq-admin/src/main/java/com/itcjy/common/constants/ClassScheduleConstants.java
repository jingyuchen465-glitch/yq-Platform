package com.itcjy.common.constants;

public class ClassScheduleConstants {

    private ClassScheduleConstants() {
    }

    /**
     * 默认排课规则：周一、二、三、五、六上课，周四自习，周日休息，法定节假日休息。
     */
    public static final String RULE_CONFIG = """
            {
              "classDays": [1, 2, 3, 5, 6],
              "selfStudyDays": [4],
              "restDays": [7],
              "holidayRest": true
            }
            """;

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

    /**
     * 课表规则配置 JSON 字段名
     */
    public static final class RuleField {
        private RuleField() {
        }

        /** 上课日（星期几集合，1=周一 ... 7=周日） */
        public static final String CLASS_DAYS = "classDays";
        /** 自习日（星期几集合） */
        public static final String SELF_STUDY_DAYS = "selfStudyDays";
        /** 固定休息日（星期几集合） */
        public static final String REST_DAYS = "restDays";
        /** 法定节假日是否自动休息 */
        public static final String HOLIDAY_REST = "holidayRest";
    }
}
