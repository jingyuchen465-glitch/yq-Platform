package com.itcjy.emp.controller.pay;

import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.service.pay.IAlipayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/pay/alipay")
@Tag(name = "Alipay callback")
public class AlipayController {

    private final IAlipayService alipayService;

    @Operation(summary = "Alipay asynchronous notification")
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        return alipayService.handleNotify(extractParams(request)) ? "success" : "failure";
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, String.join(",", values)));
        return params;
    }
}
