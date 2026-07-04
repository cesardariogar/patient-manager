#!/bin/bash

set -e # stops script if something fail

aws --endpoint-url=http://localhost:4566 cloudformation deploy \
    --stack-name patient-management \
    --template-file "./cdk.out/localstack.template.json"

# aws --endpoint-url http://localhost:4566 elbv2 describe-load-balancers \
#    --query "LoadBalancers[0].DNSName" --output text

aws --endpoint-url=http://localhost:4566 cloudformation describe-stacks \
    --stack-name patient-management \
    --query "Stacks[0].Outputs[?OutputKey=='ApiGatewayServiceLoadBalancerDNS'].OutputValue" \
    --output text