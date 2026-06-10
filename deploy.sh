#!/bin/bash
# =====================================================
#  전체 빌드 & 쿠버네티스 배포 스크립트
#  사용법: ./deploy.sh [build|deploy|all|down]
# =====================================================

set -e  # 에러 발생 시 즉시 중단

# 색상 출력
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${GREEN}[✓] $1${NC}"; }
warn() { echo -e "${YELLOW}[!] $1${NC}"; }
err() { echo -e "${RED}[✗] $1${NC}"; exit 1; }

# 프로젝트 루트 기준으로 실행
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ── 1. Docker 이미지 빌드 ─────────────────────────────
build_images() {
  log "Docker 이미지 빌드 시작..."

  SERVERS=("config-server" "gateway-server" "backend" "chat-server" "batch-server")

  for server in "${SERVERS[@]}"; do
    if [ ! -d "$server" ]; then
      warn "$server 디렉토리 없음, 스킵"
      continue
    fi
    log "$server 빌드 중..."
    docker build -t "$server:latest" "./$server"
    log "$server 빌드 완료"
  done

  log "전체 이미지 빌드 완료!"
  docker images | grep -E "config-server|gateway-server|backend|chat-server|batch-server"
}

# ── 2. 인프라 (docker-compose) 실행 ──────────────────
start_infra() {
  log "인프라 컨테이너 실행 중 (MySQL, Redis, Kafka, ES, Prometheus, Grafana)..."
  docker-compose up -d
  log "인프라 기동 완료. 30초 대기..."
  sleep 30
}

# ── 3. metrics-server 설치 (HPA 필수) ────────────────
install_metrics_server() {
  log "metrics-server 설치 확인 중..."
  if ! kubectl get deployment metrics-server -n kube-system &>/dev/null; then
    warn "metrics-server 없음. 설치 중..."
    kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
    # Docker Desktop / minikube 환경에서는 TLS 검증 비활성화 필요
    kubectl patch deployment metrics-server -n kube-system \
      --type='json' \
      -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
    log "metrics-server 설치 완료. 30초 대기..."
    sleep 30
  else
    log "metrics-server 이미 설치되어 있음"
  fi
}

# ── 4. 쿠버네티스 배포 ────────────────────────────────
deploy_k8s() {
  log "쿠버네티스 배포 시작..."

  # 순서 중요: config-server 먼저 → 나머지 서버들
  log "1) config-server 배포..."
  kubectl apply -f config-server/k8s/configmap.yaml
  kubectl apply -f config-server/k8s/config-server.yaml
  log "config-server 준비 대기 (40초)..."
  kubectl wait --for=condition=ready pod -l app=config-server --timeout=120s || warn "config-server 아직 기동 중..."
  sleep 10

  log "2) gateway-server 배포..."
  kubectl apply -f gateway-server/k8s/gateway-server.yaml

  log "3) backend 배포..."
  kubectl apply -f backend/k8s/backend.yaml

  log "4) chat-server 배포..."
  kubectl apply -f chat-server/k8s/chat-server.yaml

  log "5) batch-server 배포..."
  kubectl apply -f batch-server/k8s/batch-server.yaml

  log "전체 배포 완료! 파드 상태 확인 중..."
  sleep 15
  kubectl get pods
  kubectl get services
  kubectl get hpa
}

# ── 5. 배포 확인 ─────────────────────────────────────
check_status() {
  echo ""
  log "=== 파드 상태 ==="
  kubectl get pods -o wide

  echo ""
  log "=== 서비스 목록 ==="
  kubectl get services

  echo ""
  log "=== HPA 상태 (오토스케일링) ==="
  kubectl get hpa

  echo ""
  log "=== 접근 주소 ==="
  echo "  API Gateway : http://localhost:30000"
  echo "  Prometheus  : http://localhost:9090"
  echo "  Grafana     : http://localhost:3000 (admin/admin)"
}

# ── 6. 전체 종료 ─────────────────────────────────────
teardown() {
  warn "쿠버네티스 리소스 삭제 중..."
  kubectl delete -f backend/k8s/backend.yaml --ignore-not-found
  kubectl delete -f gateway-server/k8s/gateway-server.yaml --ignore-not-found
  kubectl delete -f chat-server/k8s/chat-server.yaml --ignore-not-found
  kubectl delete -f batch-server/k8s/batch-server.yaml --ignore-not-found
  kubectl delete -f config-server/k8s/config-server.yaml --ignore-not-found

  warn "인프라 컨테이너 종료 중..."
  docker-compose down

  log "전체 종료 완료"
}

# ── 실행 ─────────────────────────────────────────────
case "${1:-all}" in
  build)
    build_images
    ;;
  infra)
    start_infra
    ;;
  deploy)
    install_metrics_server
    deploy_k8s
    check_status
    ;;
  all)
    build_images
    start_infra
    install_metrics_server
    deploy_k8s
    check_status
    ;;
  status)
    check_status
    ;;
  down)
    teardown
    ;;
  *)
    echo "사용법: ./deploy.sh [build|infra|deploy|all|status|down]"
    echo "  build  : Docker 이미지만 빌드"
    echo "  infra  : 인프라(docker-compose)만 실행"
    echo "  deploy : k8s에 서버 배포"
    echo "  all    : 빌드 + 인프라 + 배포 전체 실행 (기본값)"
    echo "  status : 현재 상태 확인"
    echo "  down   : 전체 종료 및 삭제"
    ;;
esac