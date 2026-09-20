#!/usr/bin/env bash
set -euo pipefail

# ==========================
# CONFIG (edit these first)
# ==========================
AWS_REGION="${AWS_REGION:-eu-central-1}"
INSTANCE_NAME="${INSTANCE_NAME:-vektrlabs-prod-app}"
INSTANCE_TYPE="${INSTANCE_TYPE:-t3.small}"
AMI_ID="${AMI_ID:-}" # Leave empty to auto-resolve latest Amazon Linux 2023 AMI
KEY_NAME="${KEY_NAME:-}" # Optional: existing EC2 key pair name for SSH
VPC_ID="${VPC_ID:-}" # Optional: default VPC is auto-selected when empty
SUBNET_ID="${SUBNET_ID:-}" # Optional: first subnet in selected VPC when empty
SECURITY_GROUP_ID="${SECURITY_GROUP_ID:-}" # Optional: auto-create/find when empty
SECURITY_GROUP_NAME="${SECURITY_GROUP_NAME:-vektrlabs-prod-sg}"
SSH_CIDR="${SSH_CIDR:-0.0.0.0/0}" # Restrict this in production
WEB_CIDR="${WEB_CIDR:-0.0.0.0/0}"
APP_PORT="${APP_PORT:-8080}"
HTTP_PORT="${HTTP_PORT:-80}"
HTTPS_PORT="${HTTPS_PORT:-443}"
ROOT_VOLUME_GB="${ROOT_VOLUME_GB:-30}"
ENABLE_EIP="${ENABLE_EIP:-false}"

CREATE_EC2_ROLE="${CREATE_EC2_ROLE:-true}"
EC2_ROLE_NAME="${EC2_ROLE_NAME:-vektrlabs-prod-ec2-role}"
INSTANCE_PROFILE_NAME="${INSTANCE_PROFILE_NAME:-vektrlabs-prod-ec2-profile}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd aws

if [[ -z "${AMI_ID}" ]]; then
  AMI_ID="$(
    aws ssm get-parameter \
      --region "${AWS_REGION}" \
      --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
      --query 'Parameter.Value' \
      --output text
  )"
fi

if [[ -z "${VPC_ID}" ]]; then
  VPC_ID="$(
    aws ec2 describe-vpcs \
      --region "${AWS_REGION}" \
      --filters Name=is-default,Values=true \
      --query 'Vpcs[0].VpcId' \
      --output text
  )"
fi

if [[ -z "${SUBNET_ID}" ]]; then
  SUBNET_ID="$(
    aws ec2 describe-subnets \
      --region "${AWS_REGION}" \
      --filters "Name=vpc-id,Values=${VPC_ID}" \
      --query 'Subnets[0].SubnetId' \
      --output text
  )"
fi

if [[ -z "${SECURITY_GROUP_ID}" ]]; then
  SECURITY_GROUP_ID="$(
    aws ec2 describe-security-groups \
      --region "${AWS_REGION}" \
      --filters "Name=vpc-id,Values=${VPC_ID}" "Name=group-name,Values=${SECURITY_GROUP_NAME}" \
      --query 'SecurityGroups[0].GroupId' \
      --output text 2>/dev/null || true
  )"
  if [[ -z "${SECURITY_GROUP_ID}" || "${SECURITY_GROUP_ID}" == "None" ]]; then
    SECURITY_GROUP_ID="$(
      aws ec2 create-security-group \
        --region "${AWS_REGION}" \
        --group-name "${SECURITY_GROUP_NAME}" \
        --description "Security group for ${INSTANCE_NAME}" \
        --vpc-id "${VPC_ID}" \
        --query GroupId \
        --output text
    )"
  fi
fi

authorize_port() {
  local port="$1"
  local cidr="$2"
  aws ec2 authorize-security-group-ingress \
    --region "${AWS_REGION}" \
    --group-id "${SECURITY_GROUP_ID}" \
    --protocol tcp \
    --port "${port}" \
    --cidr "${cidr}" >/dev/null 2>&1 || true
}

authorize_port "${APP_PORT}" "${WEB_CIDR}"
authorize_port "${HTTP_PORT}" "${WEB_CIDR}"
authorize_port "${HTTPS_PORT}" "${WEB_CIDR}"
authorize_port 22 "${SSH_CIDR}"

if [[ "${CREATE_EC2_ROLE}" == "true" ]]; then
  trust_file="$(mktemp)"
  cat > "${trust_file}" <<'JSON'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "Service": "ec2.amazonaws.com" },
      "Action": "sts:AssumeRole"
    }
  ]
}
JSON

  aws iam create-role \
    --role-name "${EC2_ROLE_NAME}" \
    --assume-role-policy-document "file://${trust_file}" >/dev/null 2>&1 || true
  rm -f "${trust_file}"

  aws iam attach-role-policy --role-name "${EC2_ROLE_NAME}" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore >/dev/null
  aws iam attach-role-policy --role-name "${EC2_ROLE_NAME}" --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly >/dev/null
  aws iam attach-role-policy --role-name "${EC2_ROLE_NAME}" --policy-arn arn:aws:iam::aws:policy/AmazonSSMReadOnlyAccess >/dev/null
  aws iam attach-role-policy --role-name "${EC2_ROLE_NAME}" --policy-arn arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy >/dev/null
  aws iam attach-role-policy --role-name "${EC2_ROLE_NAME}" --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess >/dev/null

  aws iam create-instance-profile --instance-profile-name "${INSTANCE_PROFILE_NAME}" >/dev/null 2>&1 || true
  aws iam add-role-to-instance-profile \
    --instance-profile-name "${INSTANCE_PROFILE_NAME}" \
    --role-name "${EC2_ROLE_NAME}" >/dev/null 2>&1 || true
fi

run_instances_args=(
  --region "${AWS_REGION}"
  --image-id "${AMI_ID}"
  --instance-type "${INSTANCE_TYPE}"
  --subnet-id "${SUBNET_ID}"
  --security-group-ids "${SECURITY_GROUP_ID}"
  --block-device-mappings "DeviceName=/dev/xvda,Ebs={VolumeSize=${ROOT_VOLUME_GB},VolumeType=gp3,DeleteOnTermination=true}"
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${INSTANCE_NAME}}]"
  --query 'Instances[0].InstanceId'
  --output text
)

if [[ -n "${KEY_NAME}" ]]; then
  run_instances_args+=(--key-name "${KEY_NAME}")
fi

if [[ "${CREATE_EC2_ROLE}" == "true" ]]; then
  run_instances_args+=(--iam-instance-profile "Name=${INSTANCE_PROFILE_NAME}")
fi

INSTANCE_ID="$(aws ec2 run-instances "${run_instances_args[@]}")"

aws ec2 wait instance-running --region "${AWS_REGION}" --instance-ids "${INSTANCE_ID}"
aws ec2 wait instance-status-ok --region "${AWS_REGION}" --instance-ids "${INSTANCE_ID}"

PUBLIC_DNS="$(
  aws ec2 describe-instances \
    --region "${AWS_REGION}" \
    --instance-ids "${INSTANCE_ID}" \
    --query 'Reservations[0].Instances[0].PublicDnsName' \
    --output text
)"

PUBLIC_IP="$(
  aws ec2 describe-instances \
    --region "${AWS_REGION}" \
    --instance-ids "${INSTANCE_ID}" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text
)"

if [[ "${ENABLE_EIP}" == "true" ]]; then
  ALLOCATION_ID="$(
    aws ec2 allocate-address --region "${AWS_REGION}" --domain vpc --query AllocationId --output text
  )"
  aws ec2 associate-address \
    --region "${AWS_REGION}" \
    --instance-id "${INSTANCE_ID}" \
    --allocation-id "${ALLOCATION_ID}" >/dev/null
fi

echo "Waiting for SSM registration..."
for _ in $(seq 1 40); do
  ping_status="$(
    aws ssm describe-instance-information \
      --region "${AWS_REGION}" \
      --filters "Key=InstanceIds,Values=${INSTANCE_ID}" \
      --query 'InstanceInformationList[0].PingStatus' \
      --output text 2>/dev/null || true
  )"
  if [[ "${ping_status}" == "Online" ]]; then
    break
  fi
  sleep 5
done

bootstrap_command_id="$(
  aws ssm send-command \
    --region "${AWS_REGION}" \
    --instance-ids "${INSTANCE_ID}" \
    --document-name AWS-RunShellScript \
    --comment "Bootstrap ${INSTANCE_NAME}" \
    --parameters 'commands=[
      "set -euo pipefail",
      "sudo dnf install -y docker git jq awscli",
      "sudo dnf install -y docker-compose-plugin || true",
      "sudo systemctl enable --now docker",
      "sudo usermod -aG docker ec2-user",
      "sudo mkdir -p /opt/vektrlabs/scripts",
      "sudo chown -R ec2-user:ec2-user /opt/vektrlabs"
    ]' \
    --query 'Command.CommandId' \
    --output text
)"

aws ssm wait command-executed \
  --region "${AWS_REGION}" \
  --command-id "${bootstrap_command_id}" \
  --instance-id "${INSTANCE_ID}"

echo ""
echo "EC2 provisioned successfully."
echo "INSTANCE_ID=${INSTANCE_ID}"
echo "PUBLIC_IP=${PUBLIC_IP}"
echo "PUBLIC_DNS=${PUBLIC_DNS}"
echo "AWS_REGION=${AWS_REGION}"
echo "SECURITY_GROUP_ID=${SECURITY_GROUP_ID}"
echo "INSTANCE_PROFILE_NAME=${INSTANCE_PROFILE_NAME}"
