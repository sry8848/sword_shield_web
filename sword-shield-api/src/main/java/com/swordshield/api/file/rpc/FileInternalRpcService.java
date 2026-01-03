package com.swordshield.api.file.rpc;

/**
 * 文件服务内部 RPC 接口。
 *
 * <p>用于其它服务（如内容服务）在服务端侧上传文件并获取预签名 URL；该接口通常不直接暴露给外部客户端。
 */
public interface FileInternalRpcService {
    /**
     * 内部服务上传文件并返回可访问的预签名 URL（常用于内容服务生成海报/媒体资源）。
     *
     * @param bytes         文件内容
     * @param filename      原始文件名（用于保留扩展名）
     * @param contentType   MIME 类型，可为空
     * @param authorization 用户或服务 JWT（用于鉴权与归属）
     * @param expireSeconds 预签名有效期（秒）
     */
    String uploadAndPresign(byte[] bytes, String filename, String contentType, String authorization, int expireSeconds);
}
