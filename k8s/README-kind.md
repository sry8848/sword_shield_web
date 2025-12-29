# Kind 环境部署步骤

## 1) 创建 Kind 集群
```bash
kind create cluster --name sword-shield --config k8s/kind-config.yaml
```
> 已将宿主机路径 `/Users/phlin/Public/code/java/sword_shield_web/data` 挂载到 Kind 节点 `/data/host`，用于持久化 MySQL/Redis/Nacos/MinIO/Elasticsearch/Prometheus/Grafana 数据。如果修改了路径，需同步改 `k8s/kind-config.yaml` 和 `k8s/infra.yaml` 的 hostPath。

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

## 4) 使用 Helm 部署（推荐）
确保本机已安装 Helm 3，然后在仓库根目录执行：
```bash
helm install sword-shield ./helm/sword-shield \
  -n sword-shield --create-namespace
```

> 如果需要修改命名空间或镜像等参数，可编辑 `helm/sword-shield/values.yaml`，或通过 `--set` 传入。

## 4') 旧方式：直接应用 Kubernetes 清单
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/deployments.yaml
kubectl apply -f k8s/infra.yaml
```

## 5) 查看状态与访问方式
部署完成后，先检查 Pod 和 Service 状态：
```bash
kubectl get pods -n sword-shield
kubectl get svc -n sword-shield
```

如需导入内置示例数据（`db/sword_shield.sql`），可在本机通过 NodePort 连接 MySQL：
```bash
mysql -h 127.0.0.1 -P 13306 -u root -p123456 sword_shield < db/sword_shield.sql
```

如需排查某个 Pod 启动异常，可使用：
```bash
kubectl describe pod -n sword-shield <pod-name>
kubectl logs -n sword-shield <pod-name>
```

- 网关：宿主机 `http://localhost:8080` （NodePort 30080 透出）
- 前端：宿主机 `http://localhost` （NodePort 30000 透出到 80）
- Nacos 控制台：`http://localhost:8848`
- MySQL：`localhost:13306`（NodePort 30306）；图形客户端可直连
- MinIO API/Console：`http://localhost:9000` / `http://localhost:9001`
- Elasticsearch HTTP：`http://localhost:9200`
- Kibana：`http://localhost:5601`
- Grafana：`http://localhost:3000`
- Jaeger UI：`http://localhost:16686`
- 以上端口均由 Kind `extraPortMappings` ↔ NodePort 实现，需在创建集群前已写入 `k8s/kind-config.yaml`。

## 6) 注意事项
- 后端服务默认使用应用内置配置的数据库、Nacos、Redis、MinIO 等地址；如需指向 Kind 内或其他地址，请通过环境变量覆盖，如：
  ```yaml
  env:
    - name: SPRING_DATASOURCE_URL
      value: jdbc:mysql://<mysql-host>:13306/sword_shield?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    - name: SPRING_DATASOURCE_USERNAME
      value: root
    - name: SPRING_DATASOURCE_PASSWORD
      value: 123456
  ```
  可在对应 Deployment 的 `env` 下添加。
- 如果需要对接 Nacos/Redis/MinIO/ES 等，也可用同样方式添加环境变量或 ConfigMap/Secret。当前清单未内置这些基础设施组件。
- 若使用自定义镜像仓库，将镜像名改为远端仓库地址即可；若私有仓库，需在集群内创建镜像拉取 Secret 并在 Deployment 中引用。

## 7) 基础设施说明
- `k8s/infra.yaml` 启动 MySQL、Nacos、Redis、MinIO、Elasticsearch、Logstash、Kibana、Jaeger、OTel Collector、Prometheus、Grafana，均使用 `emptyDir` 临时存储（重建 Pod 数据会丢失）。
- 如需持久化，请改为 PVC/StorageClass 或 hostPath；如需外部地址，修改对应 Service/环境变量。
- Prometheus 配置已改为抓取集群内服务 (`ss-gateway`/`ss-user` 等) 的 `/actuator/prometheus`。
- 已将 Nacos、MySQL、MinIO、Elasticsearch、Kibana、Grafana、Jaeger 的 Service 设为 NodePort，并在 Kind 配置中映射到宿主机常用端口，便于直接访问控制台/接口。
- 数据持久化：MySQL、Nacos、Redis、MinIO、Elasticsearch、Prometheus、Grafana 使用 `hostPath` 挂载到 Kind 节点 `/data/host/<组件>`，该路径来自宿主机的 `./data`（通过 Kind `extraMounts`）。重建集群前请确认该宿主机目录存在且有读写权限。
