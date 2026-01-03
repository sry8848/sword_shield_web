# syntax=docker/dockerfile:1

# 前端镜像构建（多阶段）：
# 1) builder 阶段：安装依赖并执行 `vite build` 生成静态资源到 dist/
# 2) nginx 阶段：复制 dist/ 与 nginx.conf，提供静态资源服务与 API 反向代理
FROM node:20-alpine AS builder
WORKDIR /app
COPY sword-shield-web-ui/package*.json ./
RUN npm ci
COPY sword-shield-web-ui/ .
RUN npm run build

FROM nginx:1.27-alpine
COPY sword-shield-web-ui/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
