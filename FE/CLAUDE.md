# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 언어

모든 응답과 문서 작성은 한국어를 사용한다.

## 프로젝트 개요

Bun + React 19 프론트엔드 (모노레포의 `FE/` 디렉터리). 백엔드는 `../BE/`의 Spring Boot + Kotlin이며 별도 `BE/CLAUDE.md`가 있다.

번들러/러너/서버 모두 Bun을 사용한다 — Vite, Webpack, Node.js, npm을 쓰지 않는다.

## 자주 쓰는 명령어

```bash
bun install    # 의존성 설치
bun dev        # 개발 서버 (--hot, HMR + 브라우저 콘솔 서버 에코)
bun build      # 프로덕션 번들 → dist/ (BUN_PUBLIC_* 환경변수만 인라인)
bun start      # 프로덕션 모드 실행 (NODE_ENV=production)
```

테스트가 필요하면 `bun test`(Bun 내장 테스트 러너)를 사용한다. 현재 테스트 파일은 없다.

## 아키텍처

- `src/index.ts` — 진입점. `Bun.serve()`의 `routes` 객체 하나로 API 엔드포인트(`/api/*`)와 정적 서빙을 함께 정의한다. `"/*": index`가 HTML import된 `index.html`을 서빙하며, Bun이 이 HTML을 자동 번들링한다(별도 번들 설정 없음).
- `src/index.html` → `src/frontend.tsx` (React 루트, `import.meta.hot.data`로 HMR 시 루트 재사용) → `src/App.tsx`.
- 개발 모드는 `NODE_ENV !== "production"` 조건으로 `serve()`의 `development.hmr` / `development.console`이 켜진다.
- 클라이언트에 노출할 환경변수는 `BUN_PUBLIC_` 접두사 필수 (`bunfig.toml`의 `serve.static.env`와 build의 `--env` 플래그로 강제됨).
