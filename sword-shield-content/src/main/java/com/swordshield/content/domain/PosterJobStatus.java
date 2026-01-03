package com.swordshield.content.domain;

/**
 * 海报生成任务状态。
 *
 * <p>用于标识 {@link com.swordshield.content.domain.PosterJob} 的生命周期：待处理、处理中、成功、失败。
 */
public enum PosterJobStatus {
    /** 待处理（等待投递/等待处理）。 */
    PENDING,

    /** 处理中（正在生成/上传）。 */
    PROCESSING,

    /** 成功（已生成结果）。 */
    SUCCESS,

    /** 失败（生成或上传失败）。 */
    FAILED
}
