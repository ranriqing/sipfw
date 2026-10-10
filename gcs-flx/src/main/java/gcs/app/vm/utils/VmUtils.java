package gcs.app.vm.utils;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;


public class VmUtils {

   public static String toUpperCamel(String str) {
       if(StringUtils.isBlank(str)) { return null; }
       return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, str);
   }
   public static String toLowerUnderscore(String str) {
       if(StringUtils.isBlank(str)) { return null; }
       return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str);
   }
}
