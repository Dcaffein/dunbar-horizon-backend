#!/bin/bash
# 에러 발생 시 즉시 중단
set -e

# 프로젝트 디렉터리로 이동
cd /home/ubuntu/dunbarhorizon

# .env 파일에서 배포 스크립트 실행에 필요한 특정 변수만 추출
# 전체 파일을 실행(source)하지 않으므로 공백이 포함된 값도 안전함
ECR_REGISTRY=$(grep ECR_REGISTRY .env | cut -d '=' -f2-)

echo "Amazon ECR에 로그인합니다."
aws ecr get-login-password --region ap-northeast-2 | sudo docker login --username AWS --password-stdin $ECR_REGISTRY

echo "최신 도커 이미지를 다운로드합니다."
sudo docker compose pull

echo "기존 컨테이너를 중지하고 새로운 설정을 반영하여 재시작합니다"
# docker-compose.yml의 env_file 설정이 .env를 자동으로 읽어 컨테이너에 주입함
sudo docker compose down
sudo docker compose up -d

echo "사용하지 않는 도커 이미지와 리소스를 정리합니다."
sudo docker image prune -a -f

echo "배포가 성공적으로 완료되었습니다."