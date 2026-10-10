package gcs.app.sipfw.runtime;

import gcs.app.sipfw.model.SipAnalysisOptions;
import gcs.app.sipfw.model.SipIterationReport;
import gcs.app.sipfw.output.SipConsoleOutputFormatter;
import gcs.app.sipfw.service.SipAnalysisEngine;
import gcs.app.sipfw.service.SipDefinitionSnapshotService;
import gcs.app.sipfw.source.SipgrepProcessSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * SIP 运行时：应用启动后自动抓取 SIP 数据。
 */
@Component
@Slf4j
public class SipAnalysisRuntime {
    private static final String WS_TOPIC = "/topic/sipfw";

    @Getter
    private final SipAnalysisEngine engine;
    private final SipAnalysisOptions options;
    private final SipDefinitionSnapshotService definitionSnapshotService;
    private final SipConsoleOutputFormatter outputFormatter;
    private final SimpMessageSendingOperations brokerMessagingTemplate;
    private final ExecutorService runtimeExecutor;
    private volatile boolean started;

    public SipAnalysisRuntime(SipAnalysisOptions options,
                              SipDefinitionSnapshotService definitionSnapshotService,
                              SipConsoleOutputFormatter outputFormatter,
                              SimpMessageSendingOperations brokerMessagingTemplate) {
        this.options = options;
        this.definitionSnapshotService = definitionSnapshotService;
        this.outputFormatter = outputFormatter;
        this.brokerMessagingTemplate = brokerMessagingTemplate;
        this.engine = new SipAnalysisEngine(options, definitionSnapshotService);
        this.runtimeExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SipAnalysisConsumeThread");
            t.setDaemon(true);
            return t;
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startOnReady() {
        if (!options.isEnabled() || started) {
            return;
        }
        started = true;
        runtimeExecutor.submit(this::startReconnectLoop);
        log.info("sip-analysis runtime started, sipgrepPath={}, portRange={}, reportIntervalMs={}",
                options.getSipgrepPath(), options.getPortRange(), options.getReportIntervalMillis());
    }

    private void startReconnectLoop() {
        // 进程异常退出时自动重连，保证长期运行。
        while (!Thread.currentThread().isInterrupted()) {
            log.info("##### SIP-ANALYSIS start #####");
            sleepQuietly(options.getReportIntervalMillis());
            try (SipgrepProcessSource source = new SipgrepProcessSource(
                    options.getSipgrepPath(),
                    options.getFromFilter(),
                    options.getPortRange())) {
                engine.consumeWithIteration(source.getReader(), options.getReportIntervalMillis(), this::handleIterationReport);
            } catch (IOException e) {
                log.error("sip-analysis source io error", e);
            } catch (Exception e) {
                log.error("sip-analysis source error", e);
            }
            log.info("##### SIP-ANALYSIS end #####");
            sleepQuietly(options.getRestartDelayMillis());
        }
    }

    /**
     * 每轮统计结果同时写日志，并推送到前端订阅主题。
     */
    private void handleIterationReport(SipIterationReport report) {
        String consoleText = outputFormatter.formatReport(report);
        if (options.isConsoleLogEnabled()) {
            log.info("\n{}", consoleText);
        }
        Map<String, Object> payload = buildWsPayload(report, options);
        log.info("sip-analysis push topic={}, inboundCaller={}, inboundIp={}, outboundCaller={}, outboundIp={}",
                WS_TOPIC,
                report.getInboundCallerGroup().keySet().stream().sorted().collect(Collectors.toList()),
                report.getInboundIpGroup().keySet().stream().sorted().collect(Collectors.toList()),
                report.getOutboundCallerGroup().keySet().stream().sorted().collect(Collectors.toList()),
                report.getOutboundIpGroup().keySet().stream().sorted().collect(Collectors.toList())
        );
        // 前端订阅 /topic/sipfw 即可拿到完整统计与分组明细。
        brokerMessagingTemplate.convertAndSend(WS_TOPIC, payload);
    }

    private Map<String, Object> buildWsPayload(SipIterationReport report, SipAnalysisOptions options) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        // 统计区间范围
        payload.put("rptMillis", options.getArchiveAfterMillis());
        // 呼入通话统计
        payload.put("handling", report.getHandling());
        payload.put("handlingAnswered", report.getHandlingAnswered());
        payload.put("handlingConnected", report.getHandlingConnected());
        payload.put("handlingTrying", report.getHandlingTrying());
        payload.put("handlingAnswerRate", report.getHandlingAnswerRate());
        payload.put("handlingConnectRate", report.getHandlingConnectRate());
        payload.put("processed", report.getProcessed());
        payload.put("processedAnswerRate", report.getProcessedAnswerRate());
        payload.put("dimensionCount", report.getDimensionCount());
        payload.put("specialSipCodeJson", definitionSnapshotService.getMergedSpecialSipCodeMap());
        payload.put("inboundCallerGroup", report.getInboundCallerGroup());
        payload.put("inboundIpGroup", report.getInboundIpGroup());
        payload.put("outboundCallerGroup", report.getOutboundCallerGroup());
        payload.put("outboundIpGroup", report.getOutboundIpGroup());
        return payload;
    }

    private void sleepQuietly(long millis) {
        long safeMillis = millis > 0L ? millis : 3000L;
        try {
            TimeUnit.MILLISECONDS.sleep(safeMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void shutdown() {
        runtimeExecutor.shutdownNow();
    }
}
