package com.itcjy.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.HolidayInfo;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class HolidayUtil {

    private static final String HOLIDAY_YEAR_URL = "https://timor.tech/api/holiday/year/";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final Cache<Integer, Map<String, HolidayInfo>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(20)
            .build();

    public HolidayInfo getHolidayInfo(LocalDate date) {
        int year = date.getYear();
        try {
            Map<String, HolidayInfo> yearHolidayMap = cache.get(year, () -> loadYearHoliday(year));
            return yearHolidayMap.get(date.toString());
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof BusinessException businessException) {
                throw businessException;
            }
            throw BusinessException.REMOTE_ERROR.newInstance("节假日信息获取失败");
        }
    }

    private Map<String, HolidayInfo> loadYearHoliday(int year) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HOLIDAY_YEAR_URL + year))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw BusinessException.REMOTE_ERROR.newInstance(
                        "节假日API响应异常，HTTP " + response.statusCode());
            }
            JSONObject body = JSON.parseObject(response.body());
            if (body == null || body.getIntValue("code") != 0) {
                throw BusinessException.REMOTE_ERROR.newInstance("节假日信息获取失败");
            }

            JSONObject holiday = body.getJSONObject("holiday");
            Map<String, HolidayInfo> result = new HashMap<>();
            if (holiday == null) {
                return result;
            }

            for (String key : holiday.keySet()) {
                JSONObject item = holiday.getJSONObject(key);
                if (item == null) {
                    continue;
                }
                String dateText = item.getString("date");
                String date = dateText == null ? year + "-" + key : dateText;
                result.put(date, new HolidayInfo(item.getBoolean("holiday"), item.getString("name")));
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.REMOTE_ERROR.newInstance("节假日信息获取失败");
        }
    }

}
