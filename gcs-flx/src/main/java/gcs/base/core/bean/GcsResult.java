package gcs.base.core.bean;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.gson.Gson;
import gcs.base.core.GcsConstants;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class GcsResult {

    private String code;
    private String message;
    private Object gmodel;

    // 调用是否成功
    public boolean isSuccess() {
        return GcsState.SUCCESS.getCode().equals(this.getCode());
    }

    // 设置Model数据
    public GcsResult singleData(Object data) {
        this.gmodel = data;
        return this;
    }

    // 完善Model内容
    public GcsResult wrapPage(IPage page) {
        if(this.gmodel!=null && this.gmodel instanceof Map){
            Map<String, Object> gmodelMap = (Map<String, Object>) this.gmodel;
            gmodelMap.put(GcsConstants.PAGE_NUM, page.getCurrent());
            gmodelMap.put(GcsConstants.PAGE_SIZE, page.getSize());
            gmodelMap.put(GcsConstants.TOTAL_COUNT, page.getTotal());
            gmodelMap.put(GcsConstants.ENTITY_LIST, page.getRecords());
        }
        return this;
    }
    public GcsResult put(String key, Object value) {
        if(this.gmodel!=null && this.gmodel instanceof Map){
            Map<String, Object> gmodelMap = (Map<String, Object>) this.gmodel;
            gmodelMap.put(key, value);
        }
        return this;
    }
    public GcsResult putAll(Map<String, Object> modelMap) {
        if(this.gmodel!=null && this.gmodel instanceof Map){
            Map<String, Object> gmodelMap = (Map<String, Object>) this.gmodel;
            gmodelMap.putAll(modelMap);
        }
        return this;
    }

    public String toJson(){
        return new Gson().toJson(this);
    }

    // 调用成功结果
    public static GcsResult successResult() {
        return successResult(null);
    }
    public static GcsResult successResult(String resultMessage) {
        return stateResult(GcsState.SUCCESS, resultMessage);
    }

    // 调用失败结果
    public static GcsResult failedResult() {
        return failedResult(null);
    }
    public static GcsResult failedResult(String resultMessage) {
        return stateResult(GcsState.FAILED, resultMessage);
    }

    // 参数有误结果
    public static GcsResult paramInvalidResult() {
        return paramInvalidResult(null);
    }
    public static GcsResult paramInvalidResult(String resultMessage) {
        return stateResult(GcsState.PARAM_INVALID, resultMessage);
    }

    // 实体不存在
    public static GcsResult notExistResult() {
        return notExistResult(null);
    }
    public static GcsResult notExistResult(String resultMessage) {
        return stateResult(GcsState.NOT_EXIST, resultMessage);
    }

    // 指定类型结果
    public static GcsResult stateResult(GcsState gcsState, String resultMsg) {
        GcsResult gcsResult = new GcsResult();
        gcsResult.code = gcsState.getCode();
        gcsResult.message = resultMsg!=null?resultMsg:gcsState.getMessage();
        gcsResult.gmodel = new HashMap<>();
        return gcsResult;
    }
}
