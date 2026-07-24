package com.itcjy.emp.controller.homework;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.homework.HomeworkAnswerUploadReq;
import com.itcjy.emp.pojo.req.homework.HomeworkAnswerVisibilityReq;
import com.itcjy.emp.pojo.req.homework.HomeworkPrefillReq;
import com.itcjy.emp.pojo.req.homework.HomeworkPublishReq;
import com.itcjy.emp.pojo.req.homework.HomeworkStatusPageReq;
import com.itcjy.emp.pojo.res.homework.HomeworkClassOptionRes;
import com.itcjy.emp.pojo.res.homework.HomeworkClassStatusRes;
import com.itcjy.emp.pojo.res.homework.HomeworkPrefillRes;
import com.itcjy.emp.pojo.res.homework.HomeworkPublishRes;
import com.itcjy.emp.pojo.res.homework.HomeworkStatusItemRes;
import com.itcjy.emp.service.homework.IHomeworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/homeworks")
@Tag(name = "作业管理", description = "作业发布相关接口")
public class HomeworkController {

    private final IHomeworkService homeworkService;

    @GetMapping("/class-options")
    @Operation(summary = "查询发布作业可选班级")
    @HasPermission(code = "sys:homework:class:options", name = "查询作业班级", description = "查询发布作业可选班级")
    public ApiResponse<List<HomeworkClassOptionRes>> listClassOptions() {
        return ApiResponse.success(homeworkService.listClassOptions());
    }

    @GetMapping("/prefill")
    @Operation(summary = "预填发布作业信息", description = "校验班级、重复作业和当天课表后返回默认发布信息")
    @HasPermission(code = "sys:homework:prefill", name = "预填作业信息", description = "根据班级和日期生成作业发布草稿")
    public ApiResponse<HomeworkPrefillRes> prefill(@Valid @ParameterObject HomeworkPrefillReq req) {
        return ApiResponse.success(homeworkService.prefill(req));
    }

    @PostMapping
    @Operation(summary = "发布作业", description = "重新校验班级、重复作业和当天课表后写入作业")
    @HasPermission(code = "sys:homework:publish", name = "发布作业", description = "向指定班级发布Markdown作业")
    public ResponseEntity<ApiResponse<HomeworkPublishRes>> publish(
            @Valid @RequestBody HomeworkPublishReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("作业发布成功", homeworkService.publish(req)));
    }

    @GetMapping("/statuses")
    @Operation(summary = "分页查询班级作业发布情况", description = "按发布日期或截止日期查询每个班级的匹配作业")
    @HasPermission(code = "sys:homework:status:page", name = "查询作业发布情况", description = "分页查询班级作业发布情况")
    public ApiResponse<PageResult<HomeworkClassStatusRes>> pageStatus(
            @Valid @ParameterObject HomeworkStatusPageReq req) {
        return ApiResponse.success(homeworkService.pageStatus(req));
    }

    @PutMapping("/{homeworkId}/answer")
    @Operation(summary = "上传或替换标准答案")
    @HasPermission(code = "sys:homework:answer:upload", name = "上传标准答案", description = "上传或替换作业标准答案")
    public ApiResponse<HomeworkStatusItemRes> saveAnswer(
            @PathVariable @Positive(message = "作业ID必须大于0") Long homeworkId,
            @Valid @RequestBody HomeworkAnswerUploadReq req) {
        return ApiResponse.success("标准答案已保存", homeworkService.saveAnswer(homeworkId, req));
    }

    @PatchMapping("/{homeworkId}/answer-visibility")
    @Operation(summary = "修改标准答案学生可见性")
    @HasPermission(code = "sys:homework:answer:visibility", name = "设置答案可见性", description = "设置标准答案是否对学生可见")
    public ApiResponse<HomeworkStatusItemRes> updateAnswerVisibility(
            @PathVariable @Positive(message = "作业ID必须大于0") Long homeworkId,
            @Valid @RequestBody HomeworkAnswerVisibilityReq req) {
        return ApiResponse.success("答案可见性已更新", homeworkService.updateAnswerVisibility(homeworkId, req));
    }
}
