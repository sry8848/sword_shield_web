# Kind 环境部署步骤

## 1) 创建 Kind 集群
```bash
kind create cluster --name sword-shield --config k8s/kind-config.yaml
```

## 2) 本地构建镜像
在仓库根目录执行（如需跳过测试可保留 `-DskipTests`，镜像标签可按需调整）：
```bash
docker build -f sword-shield-gateway/Dockerfile -t ss-gateway:latest .
docker build -f sword-shield-user/Dockerfile -t ss-user:latest .
docker build -f sword-shield-activity/Dockerfile -t ss-activity:latest .
docker build -f sword-shield-content/Dockerfile -t ss-content:latest .
docker build -f sword-shield-interaction/Dockerfile -t ss-interaction:latest .
docker build -f sword-shield-file/Dockerfile -t ss-file:latest .
docker build -f sword-shield-admin/Dockerfile -t ss-admin:latest .
docker build -f sword-shield-web-ui/Dockerfile -t ss-web-ui:latest .
```

## 3) 将本地镜像导入 Kind
```bash
kind load docker-image ss-gateway:latest --name sword-shield
kind load docker-image ss-user:latest --name sword-shield
kind load docker-image ss-activity:latest --name sword-shield
kind load docker-image ss-content:latest --name sword-shield
kind load docker-image ss-interaction:latest --name sword-shield
kind load docker-image ss-file:latest --name sword-shield
kind load docker-image ss-admin:latest --name sword-shield
kind load docker-image ss-web-ui:latest --name sword-shield
```

## 4) 应用 Kubernetes 清单
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/deployments.yaml
```

## 5) 访问方式
- 网关：宿主机 `http://localhost:8080` （NodePort 30080 透出）
- 前端：宿主机 `http://localhost` （NodePort 30000 透出到 80）
- 其他服务默认 ClusterIP，通过网关或集群内访问。

## 6) 注意事项
- 后端服务默认使用应用内置配置的数据库、Nacos、Redis、MinIO 等地址；如需指向 Kind 内或其他地址，请通过环境变量覆盖，如：
  ```yaml
  env:
    - name: SPRING_DATASOURCE_URL
      value: jdbc:mysql://<mysql-host>:3306/sword_shield?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    - name: SPRING_DATASOURCE_USERNAME
      value: root
    - name: SPRING_DATASOURCE_PASSWORD
      value: 123456
  ```
  可在对应 Deployment 的 `env` 下添加。
- 如果需要对接 Nacos/Redis/MinIO/ES 等，也可用同样方式添加环境变量或 ConfigMap/Secret。当前清单未内置这些基础设施组件。
- 若使用自定义镜像仓库，将镜像名改为远端仓库地址即可；若私有仓库，需在集群内创建镜像拉取 Secret 并在 Deployment 中引用。
