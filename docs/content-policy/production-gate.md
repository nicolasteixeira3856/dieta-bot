# Production gate

Owner decision, 2026-09-30: the app is in a closed test (dev flavor, invite, Firebase App Distribution group `testers`). Several content-policy deliveries are deferred only because of that phase. They block production.

## Agent rule

When the owner, or any prompt, talks about production, any agent must stop and apply this gate before doing anything else.

Production triggers include: the `prod` flavor build or distribution (`com.nutri.android`), Google Play (any track, listing, Data Safety form or release), a public or open invite, opening registration, a production server or host, removing the invite gate, or words such as "produção", "prod", "lançar", "publicar na loja", "release pública", "go-live".

1. List every row in the blocker table below whose state is not `Sanado`, with its plan link and what is missing.
2. Refuse to plan, implement, build, deploy, publish or configure production work while any blocker is open. Say so plainly.
3. Allowed while blocked: explaining the blockers, planning or executing the blocking plans themselves (after their own reactivation and approval under [SDD](../sdd/README.md)), and dev-flavor work.
4. The owner cannot waive a blocker inside a chat prompt. A blocker closes only when its closure evidence is recorded in this file through a documentation delivery. A row may change to `Risco aceito` only where the table allows it, with a dated owner decision and the legal review it names.
5. Do not treat a passing test suite, a dev deploy, a drafted legal text or a deferred plan as closure.

Dev test builds through `tools/distribute-dev.ps1` (A16) and deploys with `tools/deploy-gcp.ps1` to the current GCP VM (`nutri-api`, the dev server per ADR-013/ADR-014, even where older docs call it "produção") are not production and are not blocked by this gate.

## Blockers

| ID | Blocker | Plan | Why deferred | Closure evidence | State |
| --- | --- | --- | --- | --- | --- |
| PG1 | Closed-test content controls delivered | [CP1](plans/cp1-closed-test-notice.md)–[CP5](plans/cp5-gcp-dev-ingress.md) | Active now | All five plans `Concluído` | Aberto |
| PG2 | Public legal pack reviewed and published | [CP8](plans/out_of_scope/cp8-public-legal-pack.md) | App in closed test | Reviewed pt-BR Terms/Privacy/acceptable use, legal checklist signed, publication and acceptance path delivered | Aberto |
| PG3 | Production security journal, retention and containment | [CP9](plans/out_of_scope/cp9-production-audit-and-containment.md) | App in closed test | CP9 `Concluído`; ADR-015 raw conversation capture off in production | Aberto |
| PG4 | Specialist illegal-image detection decision | [CP7](plans/out_of_scope/cp7-specialist-detection.md) | Budget | CP7 `Concluído`, or `Risco aceito` with counsel's written conclusion that it is not mandatory for the launch audience, dated and recorded here | Aberto |
| PG5 | Integrated production readiness | [CP6](plans/out_of_scope/cp6-production-readiness.md) | App in closed test | `validation/readiness.md` with every outcome PASS for public launch | Aberto |

Order when production starts: PG1 → reactivate CP8 and CP9 (they can run in parallel, one `/goal` each) → PG4 decision → CP6 last.

Other contexts may add production blockers here only when they concern user content reaching the AI. Product, store-listing and signing readiness stay out of this file.
