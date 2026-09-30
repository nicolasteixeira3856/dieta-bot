# Sources and applicability

Reviewed: 2026-09-30. Recheck the affected source before implementation or legal publication. These notes distinguish provider capabilities from our proposed product policy; they are not a legal opinion.

| Source | Verified point / use |
| --- | --- |
| [OpenAI safety best practices](https://developers.openai.com/api/docs/guides/safety-best-practices) | Send a stable privacy-preserving safety identifier. Application correlation does not establish a civil identity. |
| [OpenAI moderation](https://developers.openai.com/api/docs/guides/moderation) | Moderation is currently free and supports text/images, with category-specific modality coverage. `sexual/minors` and `illicit` are text-only. Unsupported image-category scores are not negative detections. Do not submit known or suspected CSAM to this endpoint for verification. |
| [OpenAI CSAM guidance](https://developers.openai.com/api/docs/guides/csam-guidance) | General moderation does not replace specialized detection. Prevention, incident handling and jurisdiction-specific reporting remain necessary. |
| [OpenAI data controls](https://developers.openai.com/api/docs/guides/your-data) | `store=False` is not Zero Data Retention. Provider abuse-monitoring and image-review exceptions must be represented accurately in privacy material. |
| [OpenAI prompt-injection guidance](https://developers.openai.com/api/docs/guides/agent-builder-safety) | Separate untrusted data from instructions, constrain outputs and combine controls. No prompt or classifier provides a perfect boundary. No Agent Builder adoption is proposed. |
| [Caddy reverse proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy) | Forwarded-header behavior must be verified at the actual public edge; do not parse an arbitrary user-supplied X-Forwarded-For as attribution evidence. |
| [Uvicorn settings](https://www.uvicorn.org/settings/) | Restrict forwarded-header trust to the actual proxy boundary. Verify the installed version and runtime configuration. |
| [LGPD](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm) | Assess legal basis, necessity, sensitive data, rights, security and international transfers. IP and pseudonymous identifiers remain potentially personal data. |
| [Marco Civil](https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2014/lei/l12965.htm) | Article 15 sets six-month access-log duties for qualifying providers. Applicability is not determined here; access records are distinct from conversation bodies. |
| [ECA Digital](https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2025/lei/l15211.htm) | Evaluate access by minors and Article 27 duties. A terms-only age declaration is not an applicability determination. |
| [Decree 12.880/2026](https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2026/decreto/d12880.htm) | Articles 36–40 distinguish reporting, restricted preservation and deletion after confirmed receipt; implementation must check the applicable operational acts and channels. |
| [ECA](https://www.planalto.gov.br/ccivil_03/leis/l8069.htm) | Criminal provisions and reporting-related exceptions need case-specific legal review. Do not equate a model flag with a finding of crime. |
| [Consumer Protection Code](https://www.planalto.gov.br/ccivil_03/leis/l8078compilado.htm) | Terms do not waive mandatory supplier duties; review Article 25 and other applicable provisions. |

## Cost assumptions

No new detection vendor subscription. OpenAI moderation being free does not make the entire pipeline free: scope/output checks use the existing paid model, image processing has costs, and infrastructure/retention consume resources. CP2 records measured calls, token usage and latency; no price or legal guarantee is hardcoded into policy.

## Legal questions for CP1

- Operating person/entity, economic activity and Article 15 applicability; required fields and retention, including whether source-port evidence is available/required for the relevant attribution process.
- Audience, access by minors, applicable age assurance and disclosure duties.
- Controller/operator roles, lawful bases, health data and international transfers to existing providers.
- Current competent reporting channel, operational deadlines, preservation scope, exceptions and deletion triggers. Do not assume a report by OpenAI discharges the app operator's duties.
- Notice/acceptance for current testers and any future public rollout. An unanswered question remains pending, not implicitly approved.
