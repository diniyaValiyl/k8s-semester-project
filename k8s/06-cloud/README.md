# Dental Clinic в Yandex Cloud

Этот этап переносит приложение из `05-securityh` в Managed Service for Kubernetes.
Инфраструктура описана OpenTofu. cert-manager, ExternalDNS и Gwin устанавливаются
через Marketplace в консоли Yandex Cloud, как требуется в задании.

Код подготовлен для запуска. Облачные ресурсы ещё не созданы: для этого нужны
личный аккаунт, активный грант, одобренная квота и купленный домен.
Проверки без аккаунта и проверки настоящего развёртывания перечислены отдельно ниже.

## Как устроено решение

OpenTofu создаёт одну VPC, одну подсеть, две группы безопасности, зональный кластер,
группу из одного прерываемого узла, Container Registry, публичную DNS-зону и четыре
сервисных аккаунта. У узла 2 vCPU с гарантированной долей 20%, 4 ГБ RAM и HDD 64 ГБ.
Четырёх гигабайт памяти достаточно для учебного приложения и трёх контроллеров;
узел может быть остановлен облаком, поэтому это учебная конфигурация.

Весь стек приложения работает в namespace `dental-clinic`. PostgreSQL остаётся
StatefulSet с headless Service, backend и frontend — Deployment с NodePort Service.
Два диска PVC по 4 ГБ создаются через `yc-network-hdd`.
При обновлении backend используется Recreate: сначала завершается старый Pod,
потом новый подключает тот же облачный RWO-диск загрузок.

Один Ingress содержит два домена: `dental.<ваш-домен>` для frontend
и `api.dental.<ваш-домен>` для backend. Оба используют один ALB и один TLS Secret.

1. cert-manager видит `yc-clusterissuer` в аннотации Ingress, подтверждает домены
   через DNS01 и записывает сертификат в Secret `dental-cloud-tls`.
2. Gwin видит класс `gwin-default`, берёт TLS Secret, загружает сертификат
   в Yandex Certificate Manager и создаёт Application Load Balancer.
3. Gwin записывает внешний IP в статус Ingress. ExternalDNS берёт этот IP и оба
   домена из Ingress, затем создаёт A-записи в Yandex Cloud DNS.
4. ALB принимает HTTPS и передаёт HTTP на NodePort Service. Nginx сохраняет
   `X-Forwarded-Proto`, поэтому Spring Boot видит исходный HTTPS-запрос.

`app` — Kustomize overlay над `05-securityh`. Патчи заменяют Ingress и параметры
хранилищ, сохраняют probes и настройки безопасности контейнеров.
Локальный self-signed ClusterIssuer и демонстрационные Secret удаляются из сборки.
Установка Marketplace-приложений и создание настоящих Secret выполняются отдельно.
Каталог `05-securityh/security` с локальными watcher/RBAC/NetworkPolicy не подключён:
его правила для ingress-nginx нельзя автоматически переносить на облачный ALB.

## 1. Аккаунт, грант, квоты и инструменты

Зарегистрируйтесь в Yandex Cloud, создайте платёжный аккаунт и активируйте доступный
стартовый грант. Проверьте в Billing его сумму и срок действия.
Выберите отдельный каталог для этой работы и сохраните cloud-id и folder-id.

В консоли откройте «Квоты», найдите общий объём HDD-дисков в `ru-central1-a`
и запросите 80 ГБ. Нужно минимум 72 ГБ: 64 ГБ загрузочного диска + два PVC по 4 ГБ.
Дождитесь одобрения. В старом примере указан диск 30 ГБ, но текущая документация
node group указывает минимум 64 ГБ. Обновление узла настроено без дополнительной VM.

Установите Yandex Cloud CLI, OpenTofu, kubectl и Docker Desktop.
Пользователь, запускающий OpenTofu, должен иметь права создавать ресурсы, сервисные
аккаунты и назначать им перечисленные роли в выбранном каталоге.
Для доступа kubectl нужны также права на Kubernetes API, например
`k8s.cluster-api.cluster-admin`; облачная роль `k8s.admin` сама их не заменяет.

Все команды ниже — PowerShell из корня репозитория, если явно не указано иное.

```powershell
yc init
yc config get cloud-id
yc config get folder-id
yc k8s list-versions
```

Кластер использует текущую версию канала REGULAR, версия узлов выбирается облаком.
Проверьте совместимость версии kubectl с созданным кластером.

## 2. Домен и значения OpenTofu

Купите доступный домен у выбранного регистратора. Проверьте и цену продления.
Зона должна принадлежать вам; `example.com` в файлах — только пример.

```powershell
Copy-Item .\k8s\06-cloud\infra\terraform.tfvars.example .\k8s\06-cloud\infra\terraform.tfvars
```

В `terraform.tfvars` заполните `cloud_id`, `folder_id`, `domain` и `admin_cidr`.
`domain` — купленный домен без https, путей и точки в конце.
`admin_cidr` — ваш публичный IPv4 с `/32`. Это доступ к API кластера на 443/6443.
Адрес `203.0.113.10/32` в примере нужно заменить на настоящий.

```powershell
$env:YC_TOKEN = yc iam create-token
$env:TF_CLI_CONFIG_FILE = (Resolve-Path .\k8s\06-cloud\infra\tofu.rc).Path
tofu -chdir=k8s/06-cloud/infra init
tofu -chdir=k8s/06-cloud/infra fmt -check
tofu -chdir=k8s/06-cloud/infra validate
tofu -chdir=k8s/06-cloud/infra plan
tofu -chdir=k8s/06-cloud/infra apply
```

Просмотрите план и подтвердите создание платных ресурсов только на время работы.
IAM-токен живёт ограниченное время; при истечении обновите `YC_TOKEN` той же командой.
OpenTofu читает его из окружения, токена в `.tf` и `.tfvars` нет.
`tofu.rc` использует официальное зеркало провайдеров Yandex Cloud.

```powershell
$ClusterId = tofu -chdir=k8s/06-cloud/infra output -raw cluster_id
$FolderId = tofu -chdir=k8s/06-cloud/infra output -raw folder_id
$Domain = tofu -chdir=k8s/06-cloud/infra output -raw domain
$RegistryId = tofu -chdir=k8s/06-cloud/infra output -raw registry_id
$DnsSaId = tofu -chdir=k8s/06-cloud/infra output -raw dns_service_account_id
$GwinSaId = tofu -chdir=k8s/06-cloud/infra output -raw gwin_service_account_id
$AlbSgId = tofu -chdir=k8s/06-cloud/infra output -raw alb_security_group_id
$SubnetId = tofu -chdir=k8s/06-cloud/infra output -raw subnet_id
```

В панели регистратора замените NS на `ns1.yandexcloud.net.` и
`ns2.yandexcloud.net.`. Дождитесь делегирования и проверьте через публичный DNS:

```powershell
Resolve-DnsName -Name $Domain -Type NS -Server 1.1.1.1
```

В ответе должны быть серверы Yandex Cloud. Перенос может занять до 72 часов.
Записи A вручную не создавайте: за них отвечает ExternalDNS.

## 3. Подключение к кластеру и ключи контроллеров

```powershell
New-Item -ItemType Directory -Force .\secrets | Out-Null
$env:KUBECONFIG = Join-Path (Get-Location) 'secrets\cloud.kubeconfig'
yc k8s cluster get-credentials $ClusterId --external --kubeconfig $env:KUBECONFIG
kubectl config current-context
kubectl get nodes
kubectl get storageclass yc-network-hdd
yc iam key create --service-account-id $DnsSaId --output .\secrets\dns-key.json
yc iam key create --service-account-id $GwinSaId --output .\secrets\gwin-key.json
```

Отдельный kubeconfig сохраняет локальный Minikube-контекст.
В новых терминалах задавайте тот же `KUBECONFIG` перед kubectl.
Папка `secrets`, локальные `.env`, `.tfvars`, state и plan исключены из Git.
Ключи создаются CLI после OpenTofu и не записываются в Terraform state.

Сервисные аккаунты и роли:

| Аккаунт | Роли в каталоге | Зачем |
| --- | --- | --- |
| dental-cluster-sa | k8s.clusters.agent, vpc.publicAdmin | Узлы, диски, сеть кластера |
| dental-nodes-sa | container-registry.images.puller | Скачивание приватных образов |
| dental-dns-sa | dns.editor | DNS01 для cert-manager и записи ExternalDNS |
| dental-gwin-sa | alb.editor, vpc.publicAdmin, certificate-manager.certificates.downloader, certificate-manager.editor, compute.viewer, k8s.viewer | Создание ALB и загрузка TLS в Certificate Manager |

## 4. Три приложения из Marketplace

В консоли откройте именно созданный кластер → «Другое» → Marketplace.
Установите следующие приложения и дождитесь у каждого статуса Deployed.

| Приложение | Namespace | Параметры |
| --- | --- | --- |
| cert-manager с плагином Yandex Cloud DNS ACME webhook | cert-manager-dns-space | Содержимое secrets/dns-key.json, FolderId, ваш email, ACME production URL |
| ExternalDNS с Webhook Yandex Cloud DNS | externaldns-space | Тот же dns-key.json и FolderId |
| Yandex Cloud Gwin | gwin-space | Содержимое secrets/gwin-key.json и FolderId |

ACME production URL: `https://acme-v02.api.letsencrypt.org/directory`.
Для отладки можно выбрать `https://acme-staging-v02.api.letsencrypt.org/directory`,
но браузер staging-сертификату не доверяет. Для финальной проверки используйте
production. После смены сервера ACME запросите перевыпуск через `cmctl renew
dental-cloud-tls -n dental-clinic`, если staging-сертификат уже был выдан.

У ExternalDNS выбирайте актуальное приложение с Webhook, старый продукт
«ExternalDNS с плагином для Yandex Cloud DNS» больше не поддерживается.
Все три приложения используют один каталог, в котором находятся кластер и DNS-зона.
Namespace по умолчанию при установке не используйте.

```powershell
kubectl get pods -n cert-manager-dns-space
kubectl get pods -n externaldns-space
kubectl get pods -n gwin-space
kubectl get clusterissuer yc-clusterissuer
kubectl get ingressclass gwin-default
```

`yc-clusterissuer` создаётся приложением cert-manager. Дополнительный self-signed
Issuer в облаке не нужен. У аккаунта Gwin обязательно должна быть роль
`certificate-manager.editor`: без неё сертификат не попадёт в Certificate Manager.

## 5. Образы приложения

```powershell
yc container registry configure-docker
docker build --platform linux/amd64 -t "cr.yandex/$RegistryId/dental-clinic-app:1.0.0" .\dental-clinic-app
docker build --platform linux/amd64 -t "cr.yandex/$RegistryId/dental-clinic-db-init:1.0.0" -f .\k8s\03-database\db-init\Dockerfile .
docker push "cr.yandex/$RegistryId/dental-clinic-app:1.0.0"
docker push "cr.yandex/$RegistryId/dental-clinic-db-init:1.0.0"
```

Аккаунт узлов скачивает эти образы по своей IAM-роли. Отдельный imagePullSecret
для Container Registry в этом же каталоге не нужен. Для следующих версий используйте
новый tag и одновременно меняйте его в Kustomize, чтобы IfNotPresent не оставил кеш.

## 6. Параметры манифестов и Secret

В `app/kustomization.yaml` замените `replace-with-registry-id` на RegistryId
в обоих `newName`.

В `app/ingress-patch.yaml` замените:

| Пример | Настоящее значение |
| --- | --- |
| dental.example.com | dental.ВАШ-ДОМЕН |
| api.dental.example.com | api.dental.ВАШ-ДОМЕН |
| replace-with-alb-security-group-id | AlbSgId |
| replace-with-subnet-id | SubnetId |

Домены нужно заменить и в `rules`, и в `tls.hosts`.
Не меняйте `tls.secretName` на ID сертификата: в этом ДЗ Secret выпускает cert-manager,
а Gwin автоматически переносит его в Certificate Manager.

```powershell
Copy-Item .\k8s\06-cloud\app\postgres.env.example .\k8s\06-cloud\app\postgres.env
Copy-Item .\k8s\06-cloud\app\backend.env.example .\k8s\06-cloud\app\backend.env
```

В postgres.env задайте новый пароль PostgreSQL. В backend.env укажите Google OAuth
параметры, если используете вход через Google. Заглушки позволяют запустить приложение,
но сам Google-вход с ними работать не будет. Для него добавьте в настройках OAuth
redirect URI `https://dental.ВАШ-ДОМЕН/login/oauth2/code/google`.

```powershell
kubectl apply -f .\k8s\05-securityh\namespace.yaml
kubectl -n dental-clinic create secret generic postgres-secret --from-env-file=.\k8s\06-cloud\app\postgres.env --dry-run=client -o yaml | kubectl apply -f -
kubectl -n dental-clinic create secret generic backend-secret --from-env-file=.\k8s\06-cloud\app\backend.env --dry-run=client -o yaml | kubectl apply -f -
```

Эти команды создают Secret через `data`, значения кодируются в Base64.
Base64 — кодирование, а не шифрование. Настоящих реквизитов в Git нет.

## 7. Проверка и применение

```powershell
$Rendered = kubectl kustomize .\k8s\06-cloud\app
if ($Rendered -match 'replace-with-|example\.com|dental\.local') { throw 'Заполните облачные параметры перед apply' }
kubectl apply --dry-run=server -k .\k8s\06-cloud\app
kubectl apply -k .\k8s\06-cloud\app
kubectl -n dental-clinic rollout status statefulset/postgres --timeout=300s
kubectl -n dental-clinic rollout status deployment/backend --timeout=300s
kubectl -n dental-clinic rollout status deployment/frontend --timeout=300s
kubectl -n dental-clinic get pods,pvc,svc,ingress
kubectl -n dental-clinic wait --for=condition=Ready certificate/dental-cloud-tls --timeout=600s
kubectl -n dental-clinic get ingress dental-clinic -w
```

После появления ADDRESS завершите наблюдение Ctrl+C.
Применяйте именно `-k app`: патчи не являются самостоятельным набором для `apply -f`.
PVC требуют новую облачную установку; изменить storageClass уже привязанного
локального PVC нельзя. Проверяйте облачный kubeconfig перед применением.

```powershell
yc alb load-balancer list --folder-id $FolderId
yc certificate-manager certificate list --folder-id $FolderId
$DnsZoneId = tofu -chdir=k8s/06-cloud/infra output -raw dns_zone_id
yc dns zone list-records $DnsZoneId
Resolve-DnsName "dental.$Domain" -Type A -Server 1.1.1.1
Resolve-DnsName "api.dental.$Domain" -Type A -Server 1.1.1.1
curl.exe --noproxy "*" -I "https://dental.$Domain"
curl.exe --noproxy "*" "https://api.dental.$Domain/api/test"
```

Обе A-записи должны указывать на ADDRESS Ingress, Certificate должен быть Ready=True,
сертификат должен присутствовать в Certificate Manager, API — вернуть status=ok.
Проверяйте HTTPS без `-k`: он скрывает проблему недоверенного сертификата.
Для доказательства работы ExternalDNS проверьте его логи и записи TXT-владения
в зоне; не создавайте A-записи вручную.

Если Certificate не готов, смотрите `kubectl -n dental-clinic describe certificate
dental-cloud-tls` и `kubectl -n dental-clinic get challenges,orders`.
Чаще всего проблема в NS-делегировании, folder-id или dns.editor.
Если Certificate готов, но ADDRESS нет, смотрите события Ingress и логи Gwin.
Ошибки `PermissionDenied` при загрузке сертификата видны в логах контроллера.
Для диагностики конкретного Deployment сначала узнайте имя через `kubectl get deploy
-n gwin-space`, затем выполните `kubectl logs -n gwin-space deployment/ИМЯ --tail=100`.
NodePort и порт 10501 разрешены от подсети ALB; интернет принимает отдельная группа
ALB на 80/443, её ID указан в Ingress.

## 8. Завершение работы и расходы

После проверки сначала удалите только Ingress этого приложения, пока Gwin ещё работает:

```powershell
kubectl -n dental-clinic delete ingress dental-clinic
yc alb load-balancer list --folder-id $FolderId
yc load-balancer network-load-balancer list --folder-id $FolderId
```

Дождитесь удаления ALB и его дочерних ресурсов. OpenTofu их не создавал и не может
правильно удалить вместо контроллера. В списках могут быть чужие ресурсы — проверяйте
их принадлежность этой работе. При политике upsert-only ExternalDNS может оставить
A/TXT-записи; удалите только записи `dental.<домен>` и `api.dental.<домен>` этой работы.

Сохраните данные БД и загруженные файлы, если они нужны. Затем в облачном контексте:

```powershell
kubectl -n dental-clinic delete deployment backend frontend
kubectl -n dental-clinic delete statefulset postgres
kubectl -n dental-clinic delete pvc backend-uploads postgres-data-postgres-0
tofu -chdir=k8s/06-cloud/infra plan -destroy
tofu -chdir=k8s/06-cloud/infra destroy
```

Удаление этих PVC уничтожает учебную БД и загрузки. Удаляйте их после сохранения
нужных данных. Перед destroy проверьте план: удалятся кластер, сеть, DNS-зона,
Registry и его образы, сервисные аккаунты. Домен у регистратора останется вашим,
но делегированная облачная зона перестанет обслуживаться.

После удаления проверьте в выделенном каталоге Compute Cloud диски и IP,
ALB/NLB, Certificate Manager, DNS и Registry. Сертификат, загруженный Gwin,
тоже может остаться — удалите его по ID, если он относится только к этой работе.
Закрытие терминала и остановка minikube не останавливают облачные ресурсы.

## Сверка с заданием

| Требование | Что подготовлено | Как подтвердить в облаке |
| --- | --- | --- |
| Регистрация и стартовый грант | Инструкция в разделе 1 | Billing: активный грант |
| Увеличение HDD-квоты | Расчёт 72 ГБ, запрос 80 ГБ | Одобренная квота выбранной зоны |
| Инфраструктура кластера | infra/*.tf | tofu apply и Ready-узел |
| Три приложения из Marketplace | Точные продукты и параметры в разделе 4 | Deployed и Running-поды |
| Купленный домен и делегирование | Публичная зона из OpenTofu, инструкция NS | Ответ NS от публичного резолвера |
| Ingress через Gwin | ingress-patch.yaml, NodePort, ALB security group | ADDRESS и ответ HTTPS |
| Сертификаты через cert-manager и сервисы Яндекса | yc-clusterissuer и роль certificate-manager.editor | Certificate Ready и сертификат в Certificate Manager |
| Записи от ExternalDNS | Домены в rules Ingress | A/TXT в Cloud DNS, логи ExternalDNS |
| Ветка и PR | Отдельная ветка feat/yandex-cloud | PR в main, слияние после проверки преподавателя |

Локально можно проверить `tofu fmt -check`, `tofu init -backend=false`,
`tofu validate` и `kubectl kustomize`. Эти проверки подтверждают конфигурацию,
но не заменяют реальную проверку IAM, квот, DNS01, ALB и HTTPS после создания облака.

При подготовке выполнены OpenTofu fmt/validate с провайдером Yandex 0.244.0,
строгая проверка всех 12 объектов через kubeconform по схемам Kubernetes 1.34.0
и проверка связей Service, Deployment, ConfigMap, Secret и Ingress.
Реальная облачная установка и выдача сертификата пока не проверялись.

## Материалы

- [Кластер в облаке — база знаний](https://docs.simple-env.site/Enterprise/First-semester/K8s-cloud-cluster)
- [Домен и Ingress — база знаний](https://docs.simple-env.site/Enterprise/First-semester/K8s-cloud-ingress)
- [OpenTofu — база знаний](https://docs.simple-env.site/Enterprise/First-semester/K8s-opentofu)
- [Установка Gwin](https://yandex.cloud/ru/docs/application-load-balancer/tools/gwin/quickstart)
- [Установка cert-manager с DNS webhook](https://yandex.cloud/ru/docs/managed-kubernetes/operations/applications/cert-manager-cloud-dns)
- [Установка ExternalDNS с Webhook](https://yandex.cloud/ru/docs/managed-kubernetes/operations/applications/externaldns)
- [Диски node group](https://yandex.cloud/ru/docs/terraform/resources/kubernetes_node_group)
