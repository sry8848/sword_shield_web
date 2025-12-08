# syntax=docker/dockerfile:1

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
