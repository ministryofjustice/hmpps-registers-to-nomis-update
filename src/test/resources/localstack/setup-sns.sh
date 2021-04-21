#!/usr/bin/env bash
export AWS_ACCESS_KEY_ID=foobar
export AWS_SECRET_ACCESS_KEY=foobar
export AWS_DEFAULT_REGION=eu-west-2
aws --endpoint-url=http://localhost:4566 sns create-topic --name hmpps-domain-events

aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name hmpps_registers_to_nomis_dlq
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name hmpps_registers_to_nomis_queue
aws --endpoint-url=http://localhost:4566 sqs set-queue-attributes --queue-url "http://localhost:4566/queue/hmpps_registers_to_nomis_queue" --attributes '{"RedrivePolicy":"{\"maxReceiveCount\":\"3\", \"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:hmpps_registers_to_nomis_dlq\"}"}'
aws --endpoint-url=http://localhost:4566 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:hmpps-domain-events \
    --protocol sqs \
    --notification-endpoint http://localhost:4566/queue/hmpps_registers_to_nomis_queue \
    --attributes '{"FilterPolicy":"{\"eventType\":[ \"COURT_REGISTER_UPDATE\", \"COURT_REGISTER_INSERT\"] }"}'

echo All Ready