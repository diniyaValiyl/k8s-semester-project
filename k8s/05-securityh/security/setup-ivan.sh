#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

openssl genrsa -out ivan.key 2048
openssl req -new -key ivan.key -out ivan.csr -subj "/CN=ivan/O=developers"

CSR_B64=$(cat ivan.csr | base64 | tr -d "\n")

cat > ivan-csr.yaml <<EOF
apiVersion: certificates.k8s.io/v1
kind: CertificateSigningRequest
metadata:
  name: ivan-csr
spec:
  request: "${CSR_B64}"
  signerName: kubernetes.io/kube-apiserver-client
  expirationSeconds: 86400
  usages:
    - client auth
EOF

kubectl apply -f ivan-csr.yaml

echo "=== Subject в CSR (ожидаем CN=ivan, O=developers) ==="
kubectl get csr ivan-csr -o jsonpath='{.spec.request}' | base64 -d | openssl req -noout -subject

kubectl certificate approve ivan-csr
kubectl get csr ivan-csr -o jsonpath='{.status.certificate}' | base64 -d > ivan.crt
openssl x509 -in ivan.crt -noout -subject -dates

kubectl config set-credentials ivan \
  --client-certificate=ivan.crt --client-key=ivan.key --embed-certs=true
kubectl config set-context ivan-context \
  --cluster=security --namespace=dental-clinic --user=ivan

echo "Готово. Проверь: kubectl --context=ivan-context auth whoami"