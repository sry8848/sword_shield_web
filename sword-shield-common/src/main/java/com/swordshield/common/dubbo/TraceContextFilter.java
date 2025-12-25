package com.swordshield.common.dubbo;

import com.swordshield.common.TraceUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Dubbo 内部 RPC 的 trace 上下文透传与日志关联：
 * - Consumer：将当前 traceparent / X-Trace-Id 写入 Dubbo attachments
 * - Provider：从 attachments 恢复到 MDC，保证日志可关联
 *
 * 说明：该 Filter 仅负责“传播/关联”，不负责创建 span（span 由 Micrometer/Javaagent 等负责）。
 */
public class TraceContextFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        String side = invoker.getUrl().getParameter(CommonConstants.SIDE_KEY);
        boolean isConsumer = CommonConstants.CONSUMER_SIDE.equals(side);
        boolean isProvider = CommonConstants.PROVIDER_SIDE.equals(side);

        if (isConsumer) {
            inject(invocation);
            return invoker.invoke(invocation);
        }

        if (isProvider) {
            return withMdc(invocation, () -> invoker.invoke(invocation));
        }

        return invoker.invoke(invocation);
    }

    private void inject(Invocation invocation) {
        Optional<String> traceParent = TraceUtils.currentTraceParent();
        if (traceParent.isPresent()) {
            invocation.setAttachment(TraceUtils.TRACEPARENT_HEADER, traceParent.get());
        } else {
            TraceUtils.currentTraceId()
                    .map(TraceUtils::ensureW3cTraceId)
                    .ifPresent(traceId -> invocation.setAttachment(TraceUtils.TRACEPARENT_HEADER, TraceUtils.newTraceParent(traceId)));
        }

        TraceUtils.currentTraceId()
                .map(TraceUtils::ensureW3cTraceId)
                .ifPresent(traceId -> invocation.setAttachment(TraceUtils.TRACE_ID_HEADER, traceId));
    }

    private Result withMdc(Invocation invocation, SupplierWithResult supplier) {
        String incomingTraceId = invocation.getAttachment(TraceUtils.TRACE_ID_HEADER);
        String incomingTraceParent = invocation.getAttachment(TraceUtils.TRACEPARENT_HEADER);

        String traceId = resolveTraceId(incomingTraceId, incomingTraceParent);
        if (!StringUtils.hasText(traceId)) {
            return supplier.get();
        }

        String prevTraceId = MDC.get(TraceUtils.TRACE_ID_MDC_KEY);
        String prevOtelTraceId = MDC.get(TraceUtils.OTEL_TRACE_ID_MDC_KEY);

        boolean setTraceId = !StringUtils.hasText(prevTraceId);
        boolean setOtelTraceId = !StringUtils.hasText(prevOtelTraceId);

        if (setTraceId) {
            MDC.put(TraceUtils.TRACE_ID_MDC_KEY, traceId);
        }
        if (setOtelTraceId) {
            MDC.put(TraceUtils.OTEL_TRACE_ID_MDC_KEY, traceId);
        }

        // 让下游在同一线程中还能取到 traceId（例如 HTTP client interceptor）
        RpcContext.getServerAttachment().setAttachment(TraceUtils.TRACE_ID_HEADER, traceId);
        String traceParentToUse = TraceUtils.currentTraceParent()
                .orElseGet(() -> TraceUtils.isValidTraceParent(incomingTraceParent) ? incomingTraceParent.trim() : TraceUtils.newTraceParent(traceId));
        RpcContext.getServerAttachment().setAttachment(TraceUtils.TRACEPARENT_HEADER, traceParentToUse);

        try {
            return supplier.get();
        } finally {
            if (setTraceId) {
                String current = MDC.get(TraceUtils.TRACE_ID_MDC_KEY);
                if (traceId.equals(current)) {
                    MDC.remove(TraceUtils.TRACE_ID_MDC_KEY);
                }
            }
            if (setOtelTraceId) {
                String current = MDC.get(TraceUtils.OTEL_TRACE_ID_MDC_KEY);
                if (traceId.equals(current)) {
                    MDC.remove(TraceUtils.OTEL_TRACE_ID_MDC_KEY);
                }
            }
        }
    }

    private String resolveTraceId(String incomingTraceId, String incomingTraceParent) {
        if (StringUtils.hasText(incomingTraceId)) {
            return TraceUtils.ensureW3cTraceId(incomingTraceId);
        }
        return TraceUtils.traceIdFromTraceParent(incomingTraceParent).orElse(null);
    }

    @FunctionalInterface
    private interface SupplierWithResult {
        Result get();
    }
}
