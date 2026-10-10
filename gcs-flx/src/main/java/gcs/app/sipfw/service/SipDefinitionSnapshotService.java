package gcs.app.sipfw.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gcs.app.sipfw.domain.SipPrefixDefinition;
import gcs.app.sipfw.mapper.SipPrefixDefinitionMapper;
import gcs.app.sipfw.model.CallDirection;
import gcs.app.sipfw.model.SipDefinitionCallDirection;
import gcs.app.sipfw.model.SipDefinitionSnapshot;
import gcs.app.sipfw.model.SipAnalysisOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SIP 定义快照缓存服务：合并 DB + YAML 配置，并提供匹配能力。
 */
@Slf4j
@Service
public class SipDefinitionSnapshotService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SipAnalysisOptions options;
    private final SipPrefixDefinitionMapper definitionMapper;
    private final AtomicReference<SnapshotCache> cacheRef = new AtomicReference<SnapshotCache>(SnapshotCache.empty());

    public SipDefinitionSnapshotService(SipAnalysisOptions options, SipPrefixDefinitionMapper definitionMapper) {
        this.options = options;
        this.definitionMapper = definitionMapper;
    }

    @PostConstruct
    public void init() {
        invalidateAndReload();
    }

    @Scheduled(fixedDelayString = "${sip.analysis.definition-refresh-millis:60000}")
    public void scheduledRefresh() {
        invalidateAndReload();
    }

    public synchronized void invalidateAndReload() {
        // 保存旧的质检失败记录
        Map<String, List<SipDefinitionSnapshot.SipQaFailRecord>> oldFailRecords = new LinkedHashMap<String, List<SipDefinitionSnapshot.SipQaFailRecord>>();
        SnapshotCache oldCache = cacheRef.get();
        if (oldCache != null) {
            for (SipDefinitionSnapshot old : oldCache.getSnapshots()) {
                if (old.getQaFailRecords() != null && !old.getQaFailRecords().isEmpty()) {
                    String key = buildMergeKey(old);
                    oldFailRecords.put(key, new ArrayList<SipDefinitionSnapshot.SipQaFailRecord>(old.getQaFailRecords()));
                }
            }
        }

        List<SipDefinitionSnapshot> snapshots = loadMergedSnapshots();
        // 恢复旧快照中的质检失败记录
        for (SipDefinitionSnapshot item : snapshots) {
            String key = buildMergeKey(item);
            List<SipDefinitionSnapshot.SipQaFailRecord> oldRecords = oldFailRecords.get(key);
            if (oldRecords != null) {
                item.setQaFailRecords(new ArrayList<SipDefinitionSnapshot.SipQaFailRecord>(oldRecords));
            }
        }

        Map<String, String> mergedSpecialMap = mergeSpecialCodeMap(snapshots);
        SnapshotCache newCache = new SnapshotCache(snapshots, mergedSpecialMap, System.currentTimeMillis());
        cacheRef.set(newCache);
    }

    public List<SipDefinitionSnapshot> getCurrentSnapshots() {
        return cacheRef.get().getSnapshots();
    }

    public Map<String, String> getMergedSpecialSipCodeMap() {
        return cacheRef.get().getMergedSpecialCodeMap();
    }

    public long getCacheLoadedAtMillis() {
        return cacheRef.get().getLoadedAtMillis();
    }

    /**
     * 向匹配的前缀定义添加一条质检失败记录。
     */
    public synchronized void addQaFailRecord(String number, CallDirection direction, String targetIp,
                                              SipDefinitionSnapshot.SipQaFailRecord record) {
        SipDefinitionSnapshot def = matchDefinition(number, direction, targetIp);
        if (def != null) {
            def.getQaFailRecords().add(record);
        }
    }

    /**
     * 获取指定前缀的质检失败记录。
     */
    public List<SipDefinitionSnapshot.SipQaFailRecord> getQaFailRecords(String number, CallDirection direction, String targetIp) {
        SipDefinitionSnapshot def = matchDefinition(number, direction, targetIp);
        if (def == null) {
            return new ArrayList<SipDefinitionSnapshot.SipQaFailRecord>();
        }
        return new ArrayList<SipDefinitionSnapshot.SipQaFailRecord>(def.getQaFailRecords());
    }

    /**
     * 删除指定前缀的一条质检失败记录。
     */
    public synchronized boolean deleteQaFailRecord(String number, CallDirection direction, String targetIp, int index) {
        SipDefinitionSnapshot def = matchDefinition(number, direction, targetIp);
        if (def == null || index < 0 || index >= def.getQaFailRecords().size()) {
            return false;
        }
        def.getQaFailRecords().remove(index);
        return true;
    }

    public synchronized void clearQaFailRecords(String number, CallDirection direction, String targetIp) {
        SipDefinitionSnapshot def = matchDefinition(number, direction, targetIp);
        if (def != null) {
            def.getQaFailRecords().clear();
        }
    }

    public SipDefinitionSnapshot matchDefinition(String number, CallDirection direction, String targetIp) {
        SipDefinitionSnapshot best = null;
        for (SipDefinitionSnapshot item : getCurrentSnapshots()) {
            boolean matched = item.isIpPrefix() ? item.matchesIp(targetIp) : item.matchesNumber(number);
            if (!item.matchesDirection(direction) || !matched) {
                continue;
            }
            if (best == null || item.normalizedPrefix().length() > best.normalizedPrefix().length()) {
                best = item;
            }
        }
        return best;
    }

    private List<SipDefinitionSnapshot> loadMergedSnapshots() {
        List<SipDefinitionSnapshot> yamlList = safeYamlSnapshots();
        List<SipDefinitionSnapshot> dbList = safeDbSnapshots();
        // 合并策略：同 key(prefix + direction) 时 DB 覆盖 YAML。
        Map<String, SipDefinitionSnapshot> merged = new LinkedHashMap<String, SipDefinitionSnapshot>();
        for (SipDefinitionSnapshot item : yamlList) {
            merged.put(buildMergeKey(item), item);
        }
        for (SipDefinitionSnapshot item : dbList) {
            merged.put(buildMergeKey(item), item);
        }
        return Collections.unmodifiableList(new ArrayList<SipDefinitionSnapshot>(merged.values()));
    }

    private String buildMergeKey(SipDefinitionSnapshot item) {
        return item.normalizedPrefix() + "@" + item.getCallDirection().name();
    }

    private List<SipDefinitionSnapshot> safeYamlSnapshots() {
        List<SipDefinitionSnapshot> source = options.getDefinitionSnapshotList();
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<SipDefinitionSnapshot> target = new ArrayList<SipDefinitionSnapshot>();
        for (SipDefinitionSnapshot item : source) {
            SipDefinitionSnapshot normalized = normalizeSnapshot(item);
            if (!normalized.normalizedPrefix().isEmpty()) {
                target.add(normalized);
            }
        }
        return target;
    }

    private List<SipDefinitionSnapshot> safeDbSnapshots() {
        List<SipPrefixDefinition> entities;
        try {
            entities = definitionMapper.selectList(null);
        } catch (Exception ex) {
            log.warn("load sip_prefix_definition failed, fallback to yaml only", ex);
            return Collections.emptyList();
        }
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<SipDefinitionSnapshot> snapshots = new ArrayList<SipDefinitionSnapshot>();
        for (SipPrefixDefinition entity : entities) {
            SipDefinitionSnapshot item = new SipDefinitionSnapshot();
            item.setId(entity.getId());
            item.setPrefix(entity.getPrefix());
            item.setDisplayName(entity.getDisplayName());
            item.setCallDirection(SipDefinitionCallDirection.safeValueOf(entity.getCallDirection()));
            item.setRatioTypeEnabled(Boolean.TRUE.equals(entity.getRatioTypeEnabled()));
            item.setSpecialSipCodeJson(entity.getSpecialSipCodeJson());
            item.setSpecialSipCodeMap(parseMapFromJson(entity.getSpecialSipCodeJson()));
            item.setQaEnabled(Boolean.TRUE.equals(entity.getQaEnabled()));
            item.setQaScriptMap(parseMapFromJson(entity.getQaScriptJson()));
            SipDefinitionSnapshot normalized = normalizeSnapshot(item);
            if (!normalized.normalizedPrefix().isEmpty()) {
                snapshots.add(normalized);
            }
        }
        return snapshots;
    }

    private SipDefinitionSnapshot normalizeSnapshot(SipDefinitionSnapshot source) {
        SipDefinitionSnapshot target = new SipDefinitionSnapshot();
        target.setId(source == null ? null : source.getId());
        target.setPrefix(source == null ? "" : source.normalizedPrefix());
        if (source != null) {
            target.setDisplayName(source.getDisplayName());
            target.setCallDirection(source.getCallDirection() == null ? SipDefinitionCallDirection.INBOUND : source.getCallDirection());
            target.setRatioTypeEnabled(source.isRatioTypeEnabled());
            Map<String, String> parsed = source.getSpecialSipCodeMap();
            if (parsed == null || parsed.isEmpty()) {
                parsed = parseMapFromJson(source.getSpecialSipCodeJson());
            }
            target.setSpecialSipCodeMap(parsed);
            target.setSpecialSipCodeJson(toJson(parsed));
            target.setQaEnabled(source.isQaEnabled());
            Map<String, String> qaScripts = source.getQaScriptMap();
            if (qaScripts == null || qaScripts.isEmpty()) {
                qaScripts = new LinkedHashMap<String, String>();
            }
            target.setQaScriptMap(qaScripts);
        } else {
            target.setSpecialSipCodeJson("{}");
            target.setSpecialSipCodeMap(new LinkedHashMap<String, String>());
            target.setQaScriptMap(new LinkedHashMap<String, String>());
        }
        return target;
    }

    private Map<String, String> mergeSpecialCodeMap(List<SipDefinitionSnapshot> snapshots) {
        Map<String, String> merged = new LinkedHashMap<String, String>();
        for (SipDefinitionSnapshot item : snapshots) {
            merged.putAll(item.getSpecialSipCodeMap());
        }
        return merged;
    }

    private Map<String, String> parseMapFromJson(String jsonText) {
        String source = jsonText == null ? "{}" : jsonText.trim();
        if (source.isEmpty()) {
            return new LinkedHashMap<String, String>();
        }
        try {
            Map<String, String> parsed = OBJECT_MAPPER.readValue(source, new TypeReference<Map<String, String>>() {});
            Map<String, String> result = new LinkedHashMap<String, String>();
            if (parsed == null) {
                return result;
            }
            for (Map.Entry<String, String> entry : parsed.entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().trim();
                String value = entry.getValue() == null ? "" : entry.getValue().trim();
                if (!key.isEmpty() && !value.isEmpty()) {
                    result.put(key, value);
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("parse specialSipCodeJson failed, value={}", source);
            return new LinkedHashMap<String, String>();
        }
    }

    private String toJson(Map<String, String> map) {
        try {
            return OBJECT_MAPPER.writeValueAsString(map == null ? Collections.emptyMap() : map);
        } catch (Exception ex) {
            return "{}";
        }
    }

    @Getter
    private static class SnapshotCache {
        private final List<SipDefinitionSnapshot> snapshots;
        private final Map<String, String> mergedSpecialCodeMap;
        private final long loadedAtMillis;

        private SnapshotCache(List<SipDefinitionSnapshot> snapshots, Map<String, String> mergedSpecialCodeMap, long loadedAtMillis) {
            this.snapshots = snapshots;
            this.mergedSpecialCodeMap = mergedSpecialCodeMap;
            this.loadedAtMillis = loadedAtMillis;
        }

        private static SnapshotCache empty() {
            return new SnapshotCache(Collections.emptyList(), Collections.emptyMap(), 0L);
        }
    }
}
