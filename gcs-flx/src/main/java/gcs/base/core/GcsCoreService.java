package gcs.base.core;

import gcs.app.vm.bean.VmClass;
import gcs.app.vm.service.VmClassService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class GcsCoreService {

    private static final Pattern DOMAIN_REFERENCE_PATTERN = Pattern.compile("^(\\w+)@(\\w+)$");

    /**
     * 通过实例获取 reference 字符串
     */
    public String getReferenceIdentifier(Object object) {
        if (object != null) {
            return getRealPropertyName(object.getClass()) + "@" + getIdentifier(object);
        }
        return null;
    }

    /**
     * 通过 identifier 获取实例 i.e. "domainClass@id"
     */
    public Object getReference(String identifier) {
        if (StringUtils.hasText(identifier)) {
            String[] parts = identifier.split("@");
            if (parts.length == 2) {
                String referenceClass = parts[0];
                try{
                    Long referenceId = Long.parseLong(parts[1]);
                    return findById(referenceClass, referenceId);
                }catch (NumberFormatException e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    public Object getReference(String referenceClass, Long referenceId) {
        return findById(referenceClass, referenceId);
    }

    /**
     * 通过 identifier 获取Id i.e. "domainClass@id"
     */
    public Long getReferenceId(String identifier) {
        if (identifier != null) {
            String[] parts = identifier.split("@");
            if (parts.length == 2) {
                try{
                    return Long.parseLong(parts[1]);
                }catch (NumberFormatException e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 通过实例获取 reference identList eg: [referenceClass, referenceId]
     */
    public List<Object> getReferenceIdents(Object object) {
        if (object != null) {
            return Arrays.asList(getRealPropertyName(object.getClass()), getIdentifier(object));
        }
        return null;
    }

    /**
     * 判断字符串 reference 是否为一个有效的实例类型 (i.e. "domainClass@id").
     * @param reference
     * @return
     */
    public boolean isDomainReference(String reference) {
        if (!StringUtils.hasText(reference)) {
            return false;
        }
        return DOMAIN_REFERENCE_PATTERN.matcher(reference).matches();
    }

    /**
     * 将类名转换为 referenceClass 格式（首字母小写）
     */
    public String toReferenceClass(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    /**
     * 将 Class 转换为 referenceClass 格式（取简单类名去除随机后缀后首字母小写）
     */
    public String toReferenceClass(Class<?> clazz) {
        return getRealPropertyName(clazz);
    }

    /** ----- 工具方法 ----- */
    private String getRealPropertyName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName().split("_")[0];
        return simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
    }

    private Object getIdentifier(Object object) {
        try {
            Field idField = object.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            return idField.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object findById(String referenceClass, Serializable id) {
        VmClassService vmClassService = GcsHolder.getBean(VmClassService.class);
        VmClass vmClass = vmClassService.resolveVmClass(referenceClass);
        return vmClass.getDomainMapper().selectById(id);
    }

}
