# Making the repository public: audit and hardening

On 11 November 2026 the owner asked for the repository to be hardened,
since it is becoming public. This page records what was checked across the
whole git history, what was found, what this change adds, and the GitHub
settings the owner switches on. Nothing in the app's code changed.

## What was checked

Every commit on every branch (`git log -p --all`, about 51,000 lines),
every file ever committed, and every image.

| Check | How | Result |
|---|---|---|
| Tokens and keys | Patterns for GitHub, Google, AWS, Slack and OpenAI-style tokens and private-key blocks | None. The only matches were Kotlin names such as `HEAT_TOKEN`. |
| Passwords and secrets in added lines | `password=`, `storePassword`, `keyPassword`, `secret=`, `api_key=`, `token=` | None. The only `password` lines are the CLAUDE.md command that reads the token from the keychain at run time, which holds no secret. |
| Risky files | `.jks`, `.keystore`, `.p12`, `.pem`, `.key`, `.apk`, `.aab`, `.env`, `local.properties`, `google-services.json`, credentials files, ever committed | None. |
| Email addresses | Every address in the history | Only GitHub's no-reply address for the owner, `noreply@anthropic.com` in commit trailers, and `yanivil@github.com` in the remote URL. |
| Commit authors | `git log --all --format='%an <%ae>'` | 159 commits, all with GitHub's no-reply address. 28 carry the owner's real name as the author name; the rest say `yanivil`. |
| Phone numbers | Israeli formats (`+972`, `972…`, `05x…`, landlines) | None. |
| WhatsApp and notification identifiers | WhatsApp ids (`@s.whatsapp.net`, `@lid`, `@g.us`), notification keys, shortcut ids | None. |
| Message text and names | Hebrew text, and lines about senders and contacts | The Hebrew is HyperOS's own setting names (for example "חשמל", "אין הגבלות"). Sender lines are code and docs about the own-reply check, with no real names. |
| Device and machine details | The test phone's serial number, IP and MAC addresses, IMEI-like numbers, `/Users/…` and `/Volumes/…` paths | None. |
| Large files | Non-image files over 500 KB | None. |
| Images | All 234 images, by folder and size | 221 are emulator screenshots from the PR checks, where the messages were made up (for example SMS from 555-1234 saying "Fix check D1"); two of those were looked at. The 13 that may come from the test phone (the phone checks' 7, the right-to-left fix's 5 and the pause check's 1) were each looked at: they show only Beacon's screens, its widget and icon, and HyperOS's battery page. Their status bars show only system icons: no notifications, names or numbers. |

No image or file was ever deleted from the history, so nothing is hidden
in older commits that the current files don't show.

## What this change adds

- **`.gitignore`:** signing keys and credential files (`*.jks`,
  `*.keystore`, `*.p12`, `*.pem`, `*.key`, `keystore.properties`,
  `signing.properties`, `.env`, `.env.*`, `google-services.json`,
  `*service-account*.json`), so they can't be committed by accident. No
  file the repository already tracks matches them.
- **`SECURITY.md`:** how to report a problem privately, and what Beacon
  reads and keeps.
- **`README.md`:** a Security section and a Contributing line about
  secrets.

## GitHub settings for the owner

The repo-scoped token this project uses can't change repository settings
(GitHub answered 403), so the owner switches these on in the repository's
**Settings**:

| Setting | Where | Why |
|---|---|---|
| Visibility: public | General → Danger Zone | The owner's decision. Do this last, after the settings below. |
| Private vulnerability reporting | Security → Advanced Security (or Code security) | Lets people report problems privately, as `SECURITY.md` asks. |
| Secret scanning and push protection | Same page | GitHub refuses a push that contains a known kind of token. Free for public repositories. |
| Dependabot alerts and Dependabot security updates | Same page | Warns about libraries with known security holes, and opens a pull request to fix them. Version updates stay off: the owner approves every version change. |
| A ruleset for `main`: restrict deletions, block force pushes, require a pull request before merging (0 approvals) | Rules → Rulesets → New branch ruleset, target the default branch | Nothing reaches `main` except through a pull request, as `CLAUDE.md` requires, and its history can't be rewritten. With 0 approvals the owner can still merge their own pull requests. Rulesets need the repository to be public, or GitHub Pro. |
| Actions: disabled | Actions → General | The repository has no workflows, so nobody's pull request can run code on GitHub's machines. Switch back on if a workflow is added. |
| Automatically delete head branches | General → Pull Requests | Merged branches go away by themselves. |

## Not done

- **The owner's name in 28 commits.** Removing it would mean rewriting the
  whole history and force-pushing, which breaks every existing link and
  copy. It is the owner's own name, so it was left.
- **A licence.** Without one, the code is public to read but all rights
  stay reserved. Choosing one is the owner's decision.
