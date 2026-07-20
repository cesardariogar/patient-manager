#!/bin/bash

set -e

export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_DEFAULT_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test

TEMPLATE="./cdk.out/flocci-ec2.template.json"
S3_KEY="flocci-ec2.template.json"
STACK_NAME="patient-management"
ENDPOINT="http://localhost:4566"

export AWS_ENDPOINT_URL="$ENDPOINT"

aws s3 mb s3://tmp 2>/dev/null || true

echo "Deploying stack to floci..."
aws cloudformation deploy \
  --stack-name "$STACK_NAME" \
  --s3-bucket tmp \
  --template-file "$TEMPLATE"

echo ""
echo "Stack deployed. ALB DNS:"
aws elbv2 describe-load-balancers \
  --query "LoadBalancers[0].DNSName" --output text
