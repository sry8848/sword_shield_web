/**
 * 活动服务的领域模型（Domain）。
 *
 * <p>该包下的类用于表达活动领域的核心业务对象，通常以 JPA Entity 的形式与数据库表对应，
 * 例如活动 {@link com.swordshield.activity.domain.Activity}、报名记录
 * {@link com.swordshield.activity.domain.ActivityRegistration} 等。
 *
 * <p>Domain 主要承载「业务状态」与「持久化结构」，而对外接口/跨服务传输使用 DTO（见
 * {@code com.swordshield.activity.dto} 与 {@code com.swordshield.api.activity}）。
 */
package com.swordshield.activity.domain;

