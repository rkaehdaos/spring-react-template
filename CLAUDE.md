# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 언어

모든 응답과 문서 작성은 한국어를 사용한다.

## 저장소 구조

Spring Boot 백엔드 + React 프론트엔드 모노레포. 상세 내용(명령어, 테스트 규약, 아키텍처)은 각 디렉터리의 CLAUDE.md를 참조할 것:

- `BE/` — Spring Boot 4.x + Kotlin, Gradle. → `BE/CLAUDE.md`
- `FE/` — Bun + React 19 (Vite/npm/Node 미사용). → `FE/CLAUDE.md`
- `plans/` — 작업 계획 문서 보관용.

BE와 FE는 빌드가 완전히 분리되어 있다. 작업 대상 디렉터리에서 명령을 실행할 것 (예: Gradle은 `BE/`에서, Bun은 `FE/`에서).

## Git 규약

- 커밋 메시지는 `.gitmessage.txt` 템플릿을 따른다: `<type>(<scope>): <subject>` 형식, 한국어 subject, 이모지 타입 목록과 scope 목록은 템플릿 참조. 관련 GitHub 이슈 번호를 `(#N)` 형태로 subject에 붙이는 관례가 있다.
- 브랜치는 GitHub 이슈 기반으로 `<이슈번호>-<설명>` 형식 (예: `6-maintain-260713`), PR은 `main`으로 보낸다.
