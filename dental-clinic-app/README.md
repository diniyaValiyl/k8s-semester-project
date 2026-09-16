для удобаста 
Windows PowerShell
Copyright (C) Microsoft Corporation. All rights reserved.

PS C:\Users\rajha\OneDrive\Рабочий стол\k8s-semester-project> kubectl exec -it postgres-0 -- psql -U postgres -d oris3 -c "CREATE TABLE IF NOT EXISTS test_persist (id serial PRIMARY KEY, note text); INSERT INTO test_persist (note) VALUES ('survived');"

CREATE TABLE
INSERT 0 1
PS C:\Users\rajha\OneDrive\Рабочий стол\k8s-semester-project> kubectl delete pod postgres-0
pod "postgres-0" deleted from default namespace
PS C:\Users\rajha\OneDrive\Рабочий стол\k8s-semester-project> kubectl get pods -w
NAME                          READY   STATUS              RESTARTS   AGE
dental-app-7d84c7d8f9-6h795   1/1     Running             0          3m21s
postgres-0                    0/1     ContainerCreating   0          1s
postgres-0                    0/1     ContainerCreating   0          1s
postgres-0                    1/1     Running             0          2s

Spring Boot приложение Dental Clinic, развёрнутое в Minikube по чекпоинтам курса.

dental-clinic-app/ — код приложения
k8s/01-basic/— практика №1 (Pod nginx)
k8s/02-network — практика №2 (Deployment + Service + Namespace)
k8s/03-database/ — практика №3 (PostgreSQL StatefulSet + приложение)

Запуск
minikube start --memory 8g --cpus 4
kubectl apply -f k8s/03-database/postgres-secret.yaml
kubectl apply -f k8s/03-database/postgres-configmap.yaml
kubectl apply -f k8s/03-database/postgres-service.yaml
kubectl apply -f k8s/03-database/postgres-statefulset.yaml

kubectl create secret generic app-secret \
  --from-literal=SPRING_DATASOURCE_PASSWORD="19992006" \
  --from-literal=GOOGLE_CLIENT_ID="your-id" \
  --from-literal=GOOGLE_CLIENT_SECRET="your-secret"

kubectl apply -f k8s/03-database/app-configmap.yaml
kubectl apply -f k8s/03-database/app-deployment.yaml

minikube service dental-app --url

