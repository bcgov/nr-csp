# PROD vanity hostname and manual releases

Merging a PR deploys TEST and records the backend/frontend image digests after the TEST
workflow passes. PROD is deployed only by manually running **Release PROD** from `main`
with an existing tag on a tested main commit. Creating or pushing a tag does not deploy.
The recorded image pair is deployed by digest; PROD does not rebuild or resolve moving tags.
The release also stages that commit's templates outside the checkout so the deployer cannot
replace them with current-main templates.

## GitHub prod environment

Under **Settings → Environments → prod**, configure:

| Type | Name | Value |
| --- | --- | --- |
| Variable | `VANITY_HOST` | `csp.nrs.gov.bc.ca` |
| Secret | `VANITY_TLS_CERTIFICATE` | PEM server/leaf certificate for that hostname |
| Secret | `VANITY_TLS_KEY` | Matching PEM private key used for the CSR |
| Secret | `VANITY_TLS_CA_CERTIFICATE` | PEM issuer chain in certificate-issuer order |

Use PROD-scoped OpenShift, database and existing Cognito settings. `OC_NAMESPACE` must end
in `-prod`; do not let missing PROD settings fall back to TEST credentials. Allow releases
from `main` in the environment's branch policy, and configure required reviewers if desired.
Certificate material stays in GitHub environment secrets and must never be committed.
Check the certificate's hostname, expiry and key match before configuring these values.
Deploy logging is disabled for certificate-bearing steps.

The route is `nr-csp-frontend-prod-vanity`, targeting Service
`nr-csp-frontend-prod` on port `http`, with edge TLS and HTTP-to-HTTPS redirection.
DEV previews and TEST continue using their existing Gold hostname route.

## One-time Cognito change before the vanity cutover

Keep the existing PROD Cognito user pool and app client until the separate modern-FAM
migration. In **Cognito → User pools → the PROD app client → Login pages**, add:

| Setting | Exact URL |
| --- | --- |
| Allowed callback URLs | `https://csp.nrs.gov.bc.ca` (no trailing slash) |
| Allowed sign-out URLs | `https://csp.nrs.gov.bc.ca/logout` |

Keep the existing URLs until the new-host login/logout checks pass. The deploy workflow
uses these new URLs for PROD and keeps the existing DEV/TEST origins.

Confirm that federated logout still clears the upstream session and that any upstream
post-logout allowlist accepts the return URL. Record the manual Cognito change so a legacy
FAM Terraform apply does not overwrite it before the modern-FAM migration.
See [AWS callback and sign-out settings](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-settings-client-apps.html#cognito-user-pools-app-idp-settings-about)
and [the Cognito logout endpoint](https://docs.aws.amazon.com/cognito/latest/developerguide/logout-endpoint.html).

## Release and verify

1. Synchronize the PR with `main`, confirm its images were rebuilt from that baseline,
   merge it, and wait for the complete **Merge** workflow to succeed, including the
   release-candidate artifact. Downstream-only reruns cannot reuse an older image capture.
2. Create an annotated or lightweight tag on that tested main commit.
3. Run **Release PROD** from `main` with that tag. It verifies the commit, successful TEST
   run and attempt, manifest repository and SHA, and both immutable image references.
4. Verify `https://csp.nrs.gov.bc.ca`: trusted certificate/chain, HTTP-to-HTTPS redirect,
   API access, IDIR/Business BCeID login, and full logout. This is deployed acceptance;
   a green TEST run or successful route creation does not establish production readiness.
5. After verification, retire the old PROD Gold route `nr-csp-frontend-prod` so PROD is available
   only through the vanity host. Applying the new templates does not delete an existing
   route. Preserve its configuration for rollback before removing it.

For rollback, run **Release PROD** with another tested tag whose release-candidate artifact
is still available, retaining the required environment settings for that version. The first
eligible release tag must include this release mechanism and digest-capable templates;
older Merge runs do not contain a compatible release-candidate artifact.
