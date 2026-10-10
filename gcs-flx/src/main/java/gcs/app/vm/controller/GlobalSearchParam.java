package gcs.app.vm.controller;

import lombok.Data;

import java.util.List;

/**
 * 全局搜索请求体
 */
@Data
public class GlobalSearchParam {

    /** 搜索关键词（纯标签搜索时可为空） */
    private String keyword;

    /** 标签过滤名称列表（可空） */
    private List<String> tagNames;

    /** 待搜索的 domain 列表（menu 可见 + 可搜） */
    private List<String> domainNames;
}
