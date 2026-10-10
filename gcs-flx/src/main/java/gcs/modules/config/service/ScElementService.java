package gcs.modules.config.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.base.core.GcsConstants;
import gcs.modules.config.domain.ScConfig;
import gcs.modules.config.domain.ScElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScElementService {
    @Autowired
    private ScConfigService scConfigService;

    public ScElement getScElement(String scKey, Long id){
        ScConfig scConfig = scConfigService.getScConfigByKey(scKey);
        ScElement scElement = null;
        if(scConfig!=null){
            scElement = Db.getOne(
                    Wrappers.lambdaQuery(ScElement.class)
                            .eq(ScElement::getScConfigId, scConfig.getId())
                            .eq(ScElement::getId, id)
            );
        }
        return scElement;
    }

    public List<ScElement> getScElementTree(String scKey){
        ScConfig scConfig = scConfigService.getScConfigByKey(scKey);
        List<ScElement> scElements = new ArrayList<ScElement>();
        if(scConfig!=null){
            scElements = Db.list(
                    Wrappers.lambdaQuery(ScElement.class)
                            .eq(ScElement::getScConfigId, scConfig.getId())
                            .orderByAsc(ScElement::getSeqOrder)
            );
        }
        return _buildElementTree(scElements);
    }

    private List<ScElement> _buildElementTree(List<ScElement> elements) {
        if (CollUtil.isEmpty(elements)) {
            return Collections.emptyList();
        }
        Map<Long, ScElement> elementMap = elements.stream()
                .collect(Collectors.toMap(ScElement::getId, e -> e));
        // 存储根节点
        List<ScElement> rootElements = new ArrayList<>();
        // 遍历构建父子关系
        for (ScElement element : elements) {
            Long parentId = element.getParentId();
            if (parentId == null || parentId == 0) {
                rootElements.add(element);
            } else {
                ScElement parent = elementMap.get(parentId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(element);
                }
            }
        }
        return rootElements;
    }
}
