package com.swordshield.api.file.rpc;

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

