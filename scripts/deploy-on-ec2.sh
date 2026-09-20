#!/usr/bin/env bash
set -euo pipefail

# CONFIG
AWS_REGION="${AWS_REGION:-eu-central-1}"
APP_DIR="${APP_DIR:-/opt/vektrlabs}"
COMPOSE_FILE="${COMPOSE_FILE:-${APP_DIR}/docker-compose.prod.yml}"
ENV_OUTPUT_FILE="${ENV_OUTPUT_FILE:-${APP_DIR}/.env.production}"
SSM_PARAMETER_PATH_PREFIX="${SSM_PARAMETER_PATH_PREFIX:-/vektrlabs/prod/app/}"
ECR_REGISTRY="${ECR_REGISTRY:-}"
ECR_REPOSITORY="${ECR_REPOSITORY:-}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
APP_PORT="${APP_PORT:-8080}"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://localhost:${APP_PORT}/}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd aws
require_cmd docker
require_cmd curl

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose plugin is required." >&2
  exit 1
fi

if [[ -z "${ECR_REGISTRY}" || -z "${ECR_REPOSITORY}" ]]; then
  echo "ECR_REGISTRY and ECR_REPOSITORY are required." >&2
  exit 1
fi

mkdir -p "${APP_DIR}"
cd "${APP_DIR}"

if [[ ! -f "${APP_DIR}/scripts/render-env-from-ssm.sh" ]]; then
  echo "Missing ${APP_DIR}/scripts/render-env-from-ssm.sh" >&2
  exit 1
fi

AWS_REGION="${AWS_REGION}" \
SSM_PARAMETER_PATH_PREFIX="${SSM_PARAMETER_PATH_PREFIX}" \
OUTPUT_FILE="${ENV_OUTPUT_FILE}" \
"${APP_DIR}/scripts/render-env-from-ssm.sh"

set -a
# shellcheck disable=SC1090
source "${ENV_OUTPUT_FILE}"
set +a

required_keys=(
  APP_BASE_URL
  COGNITO_CLIENT_ID
  COGNITO_CLIENT_SECRET
  COGNITO_ISSUER_URI
  COGNITO_LOGOUT_URI
  APP_POST_LOGOUT_REDIRECT_URI
  SPRING_DATASOURCE_URL
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
  SESSION_COOKIE_SECURE
  PAYMENT_STRATEGY
)

missing_count=0
for key in "${required_keys[@]}"; do
  if [[ -z "${!key:-}" ]]; then
    echo "Missing required env var from SSM: ${key}" >&2
    missing_count=$((missing_count + 1))
  fi
done

if [[ "${missing_count}" -gt 0 ]]; then
  exit 1
fi

if [[ "${PAYMENT_STRATEGY}" == "STRIPE" ]]; then
  if [[ -z "${STRIPE_SECRET_KEY:-}" || -z "${STRIPE_PUBLISHABLE_KEY:-}" ]]; then
    echo "Stripe live deployment requires STRIPE_SECRET_KEY and STRIPE_PUBLISHABLE_KEY." >&2
    exit 1
  fi
  if [[ "${STRIPE_SECRET_KEY}" != sk_live_* || "${STRIPE_PUBLISHABLE_KEY}" != pk_live_* ]]; then
    echo "Stripe keys must be live-mode (sk_live_*/pk_live_*)." >&2
    exit 1
  fi
fi

aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

APP_IMAGE="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
export APP_IMAGE APP_PORT

docker pull "${APP_IMAGE}"
docker compose --env-file "${ENV_OUTPUT_FILE}" -f "${COMPOSE_FILE}" up -d --remove-orphans

for _ in $(seq 1 30); do
  if curl -fsS "${HEALTHCHECK_URL}" >/dev/null 2>&1; then
    echo "Deployment succeeded: ${APP_IMAGE}"
    exit 0
  fi
  sleep 2
done

echo "Deployment completed but healthcheck failed at ${HEALTHCHECK_URL}" >&2
exit 1
