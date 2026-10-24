# Coordinated Disclosure: Incomplete Fix for CVE-2020-11021 in `@actions/http-client`

**Package:** `@actions/http-client` (npm) / `packages/http-client` (`actions/toolkit`)  
**Affected versions:** `>= 1.0.8` through `<= 4.0.1` (continuous across 1.x, 2.x, 3.x, and 4.x)  
**Historical CVE relation:** Incomplete fix for CVE-2020-11021 (GHSA-9w6v-m7wp-jwg4)  
**Upstream reference:** `microsoft/typed-rest-client` PR #207 (CVE-2023-30846)  
**CVSS v3.1:** `CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N` (5.9 - Medium)  
**Evidence status:** Source-confirmed and runtime-confirmed. Downstream exploitation unproven.

---

While auditing `@actions/http-client`, I found that the patch for CVE-2020-11021 does not completely stop credential leakage on cross-origin redirects.

When a request receives an HTTP 3xx redirect to a different hostname, `HttpClient.request()` strips the `Authorization` header from the caller's raw headers dictionary. But right after stripping it, the client prepares the new request by calling its registered credential handlers (`BearerCredentialHandler`, `BasicCredentialHandler`, and `PersonalAccessTokenCredentialHandler`). These handlers have no origin awareness. They re-attach the sensitive `Authorization` header to the new request regardless of where the redirect points.

Because handlers are the recommended way to authenticate requests in this library, code using them still sends credentials across hostname boundaries on redirect.

### The Mechanism

In `packages/http-client/src/index.ts` (lines 421–432 in TypeScript; `lib/index.js` lines 305–315 in v4):

```typescript
if (parsedRedirectUrl.hostname !== parsedUrl.hostname) {
  for (const header in headers) {
    if (header.toLowerCase() === 'authorization') {
      delete headers[header]
    }
  }
}

info = this._prepareRequest(verb, parsedRedirectUrl, headers)
```

Deleting the header from `headers` works for raw caller headers. But `_prepareRequest()` (lines 609–613; `lib/index.js` lines 354–358) immediately runs:

```typescript
if (this.handlers) {
  for (const handler of this.handlers) {
    handler.prepareRequest(info.options)
  }
}
```

In `packages/http-client/src/auth.ts`, `BearerCredentialHandler` sets the header directly without checking where the request is going:

```typescript
export class BearerCredentialHandler implements ifm.RequestHandler {
  token: string
  constructor(token: string) { this.token = token }

  prepareRequest(options: http.RequestOptions): void {
    options.headers['Authorization'] = `Bearer ${this.token}`
  }
}
```

`BasicCredentialHandler` and `PersonalAccessTokenCredentialHandler` follow the exact same pattern.

### Upstream Fix and Azure DevOps Context

This problem was already addressed in upstream `microsoft/typed-rest-client` in PR #207 back in April 2020 (later tracked as CVE-2023-30846). Upstream stored the initial request hostname on the handler (`this.origin`) and aborted credential attachment if `options.host` differed from `this.origin`.

`@actions/http-client` adopted the caller header deletion in `index.ts`, but left `auth.ts` untouched.

We have real-world proof that this difference matters. When upstream introduced that origin check in `typed-rest-client`, it broke Azure DevOps clients because `dev.azure.com` redirects across origins to regional `*.visualstudio.com` endpoints. To handle that, Microsoft's `azure-devops-node-api` had to explicitly subclass `BearerCredentialHandler` with `allowCrossOriginAuthentication = true`. `@actions/http-client` never gained that check, so it silently allows cross-origin authentication by default.

### Scope Across Release Lineages (Source-Confirmed)

The defect is present across every major release lineage:

* **`v1.0.8`**: PR #27 (April 2020 / CVE-2020-11021). Has header stripping in `index.ts`, but no origin check in `auth.ts:44`.
* **`v1.x`**: `1.0.11` (`actions/http-client@0c99fd41`). Stripping at `index.ts:401-409`, missing at `auth.ts:41`.
* **`v2.x`**: `2.0.1` and `2.2.3` (`actions/toolkit` PR #1085 and PR #1799). Stripping at `index.ts:421-429`, missing at `auth.ts:47`.
* **`v3.x`**: `3.0.0` through `3.0.2` (`actions/toolkit` PR #2110, commit `9b27fa9`). Stripping at `index.ts:421-429`, missing at `auth.ts:47`.
* **`v4.x`**: `4.0.0` and `4.0.1` (`actions/toolkit` commit `c9ab4f9`). Stripping at `lib/index.js:305-311`, missing at `lib/auth.js:52`.

### Runtime Verification (Runtime-Confirmed)

I tested this on an Ubuntu runner with Node 22 using two local HTTP servers (`127.0.0.1` redirecting to `localhost`).

Passing a raw `Authorization` header directly in the request options (Method A) resulted in the header being stripped on redirect, as CVE-2020-11021 intended. But when using `BearerCredentialHandler`, `BasicCredentialHandler`, or `PersonalAccessTokenCredentialHandler` (Method B), the credentials were re-attached and captured by the redirected destination server on both HTTP 302 and 307. The behavior reproduced identically on `@actions/http-client@1.0.8` (CommonJS) and `@actions/http-client@4.0.1` (ESM).

### Downstream Exploitability Caveat

Downstream exploitation is currently unproven.

For an external attacker to exploit this in a GitHub Action, three conditions must align:
1. An action must use `HttpClient` with a credential handler.
2. The initial request must point to an attacker-controlled endpoint or a trusted service containing an open redirect.
3. The redirected destination must capture the re-attached token.

I surveyed public actions. Actions like `cowprotocol/autodeploy-action` or `fly-ci/wingman-action` accept user-configured URLs, but passing an arbitrary URL sends credentials to that destination on the initial request, which is not a redirect flaw. Other surveyed actions either target fixed endpoints (`api.github.com`, `api.render.com`) or omit credential handlers during file downloads.

Similarly, redirecting to services like AWS S3 presigned URLs does not yield a credential leak. S3 rejects requests containing unexpected `Authorization` headers with `400 InvalidArgument`, resulting in a failed download rather than token exfiltration.

The library behavior is defective and contradicts the intent of CVE-2020-11021, but I have not confirmed an end-to-end exploit chain in a published action.

### Proposed Patch

Bring `packages/http-client/src/auth.ts` into parity with the upstream pattern from `microsoft/typed-rest-client` PR #207:

```diff
diff --git a/packages/http-client/src/auth.ts b/packages/http-client/src/auth.ts
--- a/packages/http-client/src/auth.ts
+++ b/packages/http-client/src/auth.ts
@@ -1,9 +1,13 @@
 import * as http from 'http'
 import * as ifm from './interfaces.js'
 
 export class BasicCredentialHandler implements ifm.RequestHandler {
   username: string
   password: string
+  origin?: string
+  allowCrossOriginAuthentication: boolean
 
-  constructor(username: string, password: string) {
+  constructor(username: string, password: string, allowCrossOriginAuthentication: boolean = false) {
     this.username = username
     this.password = password
+    this.allowCrossOriginAuthentication = allowCrossOriginAuthentication
   }
 
   prepareRequest(options: http.RequestOptions): void {
     if (!options.headers) {
       throw Error('The request has no headers')
     }
+    const currentHost = options.host || (options as any).hostname
+    if (!this.origin && currentHost) {
+      this.origin = currentHost
+    }
+    if (!this.allowCrossOriginAuthentication && this.origin && this.origin !== currentHost) {
       return
+    }
     options.headers['Authorization'] = `Basic ${Buffer.from(
       `${this.username}:${this.password}`
     ).toString('base64')}`
   }
@@ -35,9 +47,13 @@ export class BearerCredentialHandler implements ifm.RequestHandler {
   token: string
+  origin?: string
+  allowCrossOriginAuthentication: boolean
 
-  constructor(token: string) {
+  constructor(token: string, allowCrossOriginAuthentication: boolean = false) {
     this.token = token
+    this.allowCrossOriginAuthentication = allowCrossOriginAuthentication
   }
 
   prepareRequest(options: http.RequestOptions): void {
     if (!options.headers) {
       throw Error('The request has no headers')
     }
+    const currentHost = options.host || (options as any).hostname
+    if (!this.origin && currentHost) {
+      this.origin = currentHost
     }
+    if (!this.allowCrossOriginAuthentication && this.origin && this.origin !== currentHost) {
       return
+    }
     options.headers['Authorization'] = `Bearer ${this.token}`
   }
```

### Reproduction Script

A standalone reproduction script is available in [`audit_sandbox/test_cross_host_redirect_validation.mjs`](file:///home/agybot/projects/default/audit_sandbox/test_cross_host_redirect_validation.mjs). It spins up two local HTTP servers, fires requests with synthetic canary tokens, and inspects the captured headers across redirects.

```bash
node audit_sandbox/test_cross_host_redirect_validation.mjs
```
