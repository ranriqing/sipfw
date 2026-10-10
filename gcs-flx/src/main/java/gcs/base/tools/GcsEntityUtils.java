package gcs.base.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

@Data
public class GcsEntityUtils {

    @Getter
    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Shanghai")));
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // extendJson/constraintJson 是前端可扩展的配置袋（可能含 component、cols 等 UI 键），
        // 后端字段扩展类只取自己关心的键，忽略未知字段避免反序列化报错
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static Map<String, Object> toMap(Object entity){
        return objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {});
    }
}
