# secret-solution
외부 비밀 저장소

- CLI, HTTP 지원 | grpc 통신 구현중

### Quick Start

```
docker-compose up -d --build
docker exec -it secrets-app ./go-secrets-cli
```

### Test

```
go test ./...
```

- `-v`: 각 테스트 함수의 실행 결과를 상세하게 표시

- `-race`: 고루틴 간의 경쟁 조건(race condition) 탐지

## usage

secret-solution cli
├─ secret
│  ├─ get [user_id] [namespace] [key]
│  ├─ get-raw [user_id] [namespace] [key]
│  ├─ get-all [user_id] [namespace]
│  ├─ save-raw [user_id] [namespace] [key] [value] --ttl
│  └─ save-encrypted [user_id] [namespace] [key] [value] --ttl
├─ git
│  ├─ sync
│  ├─ log [count]
│  ├─ rollback [commit_hash]
│  ├─ status
│  └─ checkout [branch_name]
├─ policy
│  ├─ get [policy_id]
│  └─ get-all
├─ user
│  └─ get-group [user_id]
└─ policy-binding
   ├─ bind [member_id] [member_type] [policy_ids]
   ├─ unbind [member_id] [member_type]
   ├─ unbind-all [policy_id]
   └─ get [member_id] [member_type]
