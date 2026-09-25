# StudyShield Admin

[![CI](https://github.com/krushnatkhawale/study-shield-backend-admin/actions/workflows/ci.yml/badge.svg)](https://github.com/krushnatkhawale/study-shield-backend-admin/actions/workflows/ci.yml)

Vaadin 24 console for the StudyShield backend: boards, classes, subjects, packs, quizzes and the question bank.

Login uses the **backend admin account** (`/api/auth/admin-signin`). There is no local `admin/admin123` user.

## Run

```bash
./gradlew :app:bootRun
```

http://localhost:8081/login

```
STUDYSHIELD_BACKEND_URL=https://study-shield-backend-komv.onrender.com
```

## Operator flow

1. **Boards** → **Classes** → **Subjects** (order with ↑↓).
2. **Packs** — create Freemium / Seasonal / Promotional / Complementary / Library; disable to hide.
3. **Quizzes** — belong to a pack. Add questions from the bank or write new ones. Remove sends the question back to the subject library (not deleted).
4. **Question bank** — edit creates a **new version**; the quiz always plays the latest.
5. **Home** — rebuild freemium catalog after pack changes so existing kids are not stuck on an old bundle snapshot.

Light/dark theme toggle is in the header. Theme files live in
`src/main/frontend/themes/studyshield/` (Vaadin looks there from the repo root).

See `study-shield-docs/docs/per-repo/admin.md` for the full map.
