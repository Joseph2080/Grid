#!/usr/bin/env bash
set -euo pipefail

# CONFIG
AWS_REGION="${AWS_REGION:-eu-central-1}"
SSM_PARAMETER_PATH_PREFIX="${SSM_PARAMETER_PATH_PREFIX:-/vektrlabs/prod/app/}"
OUTPUT_FILE="${OUTPUT_FILE:-.env.production}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd aws
require_cmd jq

if [[ "${SSM_PARAMETER_PATH_PREFIX}" != */ ]]; then
  SSM_PARAMETER_PATH_PREFIX="${SSM_PARAMETER_PATH_PREFIX}/"
fi

> "${OUTPUT_FILE}"
next_token=""
parameter_count=0

while true; do
  if [[ -n "${next_token}" ]]; then
    response="$(
      aws ssm get-parameters-by-path \
        --region "${AWS_REGION}" \
        --with-decryption \
        --recursive \
        --path "${SSM_PARAMETER_PATH_PREFIX}" \
        --next-token "${next_token}" \
        --output json
    )"
  else
    response="$(
      aws ssm get-parameters-by-path \
        --region "${AWS_REGION}" \
        --with-decryption \
        --recursive \
        --path "${SSM_PARAMETER_PATH_PREFIX}" \
        --output json
    )"
  fi

  while IFS=$'\t' read -r full_name value; do
    [[ -z "${full_name}" ]] && continue
    key="${full_name##*/}"
    key="${key//-/_}"
    key="$(printf '%s' "${key}" | tr '[:lower:]' '[:upper:]')"
    escaped="${value//\\/\\\\}"
    escaped="${escaped//\"/\\\"}"
    escaped="${escaped//$'\n'/\\n}"
    printf '%s="%s"\n' "${key}" "${escaped}" >> "${OUTPUT_FILE}"
    parameter_count=$((parameter_count + 1))
  done < <(printf '%s' "${response}" | jq -r '.Parameters[]? | [.Name, .Value] | @tsv')

  next_token="$(printf '%s' "${response}" | jq -r '.NextToken // empty')"
  [[ -z "${next_token}" ]] && break
done

if [[ "${parameter_count}" -eq 0 ]]; then
  echo "No parameters found under path: ${SSM_PARAMETER_PATH_PREFIX}" >&2
  exit 1
fi

echo "Wrote ${parameter_count} values to ${OUTPUT_FILE}."
