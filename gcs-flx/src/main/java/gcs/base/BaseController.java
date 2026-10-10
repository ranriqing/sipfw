package gcs.base;

import gcs.base.tools.GcsEntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class BaseController {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static Map<String, Object> _entityMap(Object entity){
        return GcsEntityUtils.toMap(entity);
    }
}
