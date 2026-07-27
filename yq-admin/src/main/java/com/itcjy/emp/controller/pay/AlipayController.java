package com.itcjy.emp.controller.pay;

import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.req.pay.AlipayTradeCreateReq;
import com.itcjy.emp.pojo.res.pay.AlipayTradeCreateRes;
import com.itcjy.emp.pojo.res.pay.AlipayTradeQueryRes;
import com.itcjy.emp.service.pay.IAlipayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/pay/alipay")
@Tag(name = "支付宝支付", description = "支付宝PC网站支付接口（无需登录鉴权）")
public class AlipayController {

    private final IAlipayService alipayService;

    @Operation(summary = "创建PC网站支付订单", description = "返回支付宝收银台跳转表单HTML，前端渲染后自动跳转到支付宝")
    @PostMapping("/create")
    public ApiResponse<AlipayTradeCreateRes> createTrade(@Valid @RequestBody AlipayTradeCreateReq req) {
        return ApiResponse.success(alipayService.createTradePagePay(req));
    }

    @Operation(summary = "查询订单支付状态", description = "主动向支付宝查询订单的支付状态")
    @GetMapping("/query/{outTradeNo}")
    public ApiResponse<AlipayTradeQueryRes> queryTrade(
            @Parameter(description = "商户订单号", required = true)
            @PathVariable @NotBlank(message = "商户订单号不能为空") String outTradeNo) {
        return ApiResponse.success(alipayService.queryTrade(outTradeNo));
    }

    @Operation(summary = "支付宝异步通知回调", description = "支付宝支付成功后异步通知此接口，无需登录鉴权")
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        boolean success = alipayService.handleNotify(params);
        return success ? "success" : "failure";
    }

    @Operation(summary = "支付宝同步跳转", description = "支付完成后浏览器同步回跳，仅用于页面展示，不作为支付成功依据")
    @GetMapping("/return")
    public ApiResponse<Map<String, String>> returnUrl(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        log.info("支付宝同步跳转，outTradeNo={}，tradeNo={}", params.get("out_trade_no"), params.get("trade_no"));
        return ApiResponse.success(params);
    }

    /**
     * 从 HttpServletRequest 中提取所有参数为 Map
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String[] values = entry.getValue();
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    valueStr.append(",");
                }
                valueStr.append(values[i]);
            }
            params.put(entry.getKey(), valueStr.toString());
        }
        return params;
    }
}
