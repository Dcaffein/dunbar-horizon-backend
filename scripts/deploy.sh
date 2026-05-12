#!/bin/bash
set -e

cd /home/ubuntu/dunbarhorizon

echo "환경 변수를 로드합니다."
source .env

echo "배포 스크립트 실행을 시작합니다."

echo "Amazon ECR에 로그인합니다."
aws ecr get-login-password --region ap-northeast-2 | sudo docker login --username AWS --password-stdin $ECR_REGISTRY

echo "최신 도커 이미지를 다운로드합니다."
sudo docker compose pull

echo "기존 컨테이너를 중지하고 새로운 컨테이너를 실행합니다."
sudo docker compose down
sudo docker compose up -d

echo "사용하지 않는 도커 이미지와 잔해를 청소하여 디스크 용량을 확보합니다."
sudo docker image prune -a -f

echo "배포가 성공적으로 완료되었습니다."