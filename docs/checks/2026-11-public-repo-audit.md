# Making the repository public: audit and hardening

On 11 November 2026 the owner asked for the repository to be hardened,
since it is becoming public. This page records what was checked across the
whole git history, what was found, what this change adds, and the GitHub
settings the owner switches on. Nothing in the app's code changed.

## What was checked

In the local clone: every commit on every branch (`git log -p --all`,
about 51,000 lines), every file ever committed, and every image. On GitHub:
the 27 pull requests' titles and descriptions, their comments, their
`refs/pull/*` refs, and releases.

| Check | How | Result |
|---|---|---|
| Tokens and keys | Patterns for GitHub, Google, AWS, Slack and OpenAI-style tokens and private-key blocks | None. The only matches were Kotlin names such as `HEAT_TOKEN`. |
| Passwords and secrets in added lines | `password=`, `storePassword`, `keyPassword`, `secret=`, `api_key=`, `token=` | None. The only `password` lines are the CLAUDE.md command that reads the token from the keychain at run time, which holds no secret. |
| Risky files | `.jks`, `.keystore`, `.p12`, `.pem`, `.key`, `.apk`, `.aab`, `.env`, `local.properties`, `google-services.json`, credentials files, ever committed | None. |
| Email addresses | Every address in the history | Only GitHub's no-reply address for the owner, `noreply@anthropic.com` in commit trailers, and `yanivil@github.com` in the remote URL. |
| Commit authors | `git log --all --format='%an <%ae>'` | 159 commits before this change, all with GitHub's no-reply address. 28 carry the owner's real name as the author name: they are the merges made on GitHub, which take the name from the owner's GitHub profile. The rest say `yanivil`. |
| Phone numbers | Israeli formats (`+972`, `972…`, `05x…`, landlines) | None. |
| WhatsApp and notification identifiers | WhatsApp ids (`@s.whatsapp.net`, `@lid`, `@g.us`), notification keys, shortcut ids | None. |
| Message text and names | Hebrew text, and lines about senders and contacts | The Hebrew is HyperOS's own setting names (for example "חשמל", "אין הגבלות"). Sender lines are code and docs about the own-reply check, with no real names. |
| Device and machine details | The test phone's serial number, IP and MAC addresses, IMEI-like numbers, `/Users/…` and `/Volumes/…` paths | None. The phone's model and build numbers are in the docs on purpose (see below). |
| Large files | Non-image files over 500 KB | None. |
| Images | The 234 images in the current files, by folder and size, and the image versions only older commits hold | 221 are emulator screenshots from the PR checks, where the messages were made up (for example SMS from 555-1234 saying "Fix check D1"). Seven of them were looked at, among them the lock screen, an SMS banner and Android's Notification access page. The 13 that may come from the test phone (the phone checks' 7, the right-to-left fix's 5 and the pause check's 1) were each looked at: they show only Beacon's screens, its widget and icon, and HyperOS's battery page. Their status bars show only system icons: no notifications, names or numbers. Older commits also hold 72 earlier versions of PR 1's screenshots, all emulator-sized and from 8 November, before the test phone was used; they weren't looked at one by one. The images carry no location or device data: only their size and colour space. |
| Pull requests on GitHub | The 27 pull requests' titles and descriptions (about 171,000 characters), comments, and `refs/pull/*` | All 27 are merged and have no comments. Their text holds no token, phone number, WhatsApp id, serial number or local path; the only email address is `yanivil@github.com`, and the Hebrew is HyperOS's setting names. All 27 `refs/pull/*` point to commits already in the history above. There are no releases. |
| Issues | — | Not checked: the token can't list issues. GitHub reports none open. |

Three code files were deleted along the way (`SystemBars.kt` and two
tests). The scans above include every version of every file in the
history, deleted ones too.

## Personal details left in on purpose

Going public shows these. None is a secret, and the owner can change any
of them:

- **The owner's name,** "Yaniv Bernhard", on 28 merge commits. Every later
  merge made on GitHub adds it again; changing the display name in the
  GitHub profile changes what new merges show.
- **The country:** commit times are in Israel's time zone (+0200), and
  the docs mention Israel's time zone and Hebrew settings.
- **The test phone:** its model (Redmi 15C 5G, 2508CRN2BE) and build
  numbers, in the phone checks.
- **How the project's token is stored:** `CLAUDE.md` describes the
  keychain entry Claude reads (its server, path and account name), not the
  token itself.
- **The design link,** a claude.ai artifact, in `CLAUDE.md` and six docs.
  It is private: others can't open it.

## What this change adds

- **`.gitignore`:** signing keys and credential files, so they can't be
  committed by accident: Android and Play keys (`*.jks`, `*.keystore`,
  backups of both, `*.p12`, `*.pfx`, `*.p8`, `*.pk8`, `*.pepk`, `*.pem`,
  `*.key`), SSH and GPG keys, `keystore.properties`, `signing.properties`,
  `secrets.properties`, `.env` files (but not `.env.example`), `.netrc`,
  `google-services.json`, `credentials.json`, OAuth client secrets and
  service-account files. Also local tool state: Claude Code's
  `settings.local.json` and worktrees, and the superpowers workspace. No
  file the repository already tracks matches them.
- **`SECURITY.md`:** how to report a problem privately, and what the
  Beacon app reads and keeps. It was checked against the code: the release
  build's merged manifest has no internet permission.
- **`README.md`:** a Security section and a Contributing line about
  secrets.

## GitHub settings for the owner

The repo-scoped token this project uses can't change repository settings
(GitHub answered 403), so they are switched on in the repository's
**Settings**. On a free account, several only exist for public
repositories, so it goes in two steps.

**While still private:**

| Setting | Where | Why |
|---|---|---|
| Dependabot alerts and Dependabot security updates | Security → Advanced Security (called Code security on some accounts) | Warns about libraries with known security holes, and opens a pull request to fix them. Version updates stay off: the owner approves every version change. |
| Actions: on, with approval for outside contributors | Actions → General: "Require approval for all outside collaborators" for fork pull requests, and "Read repository contents" as the default workflow permission | The repository has no workflows, so nothing runs. Actions stays on because Dependabot's security updates run on it. |
| Automatically delete head branches | General → Pull Requests | Merged branches go away by themselves. |

**Then make it public** (General → Danger Zone → Change visibility), and
straight away:

| Setting | Where | Why |
|---|---|---|
| Private vulnerability reporting | Security → Advanced Security | Lets people report problems privately, as `SECURITY.md` asks. |
| Secret scanning and push protection | Same page | GitHub scans the whole history for known kinds of token, and refuses a push that contains one. Free for public repositories. Afterwards, check Security → Secret scanning: it should show no alerts. |
| A ruleset for `main` | Rules → Rulesets → New ruleset → New branch ruleset. Enforcement status: **Active** (a new ruleset starts as Disabled). Target: Include default branch. Rules: Restrict deletions, Block force pushes, Require a pull request before merging, with 0 required approvals. Leave the bypass list empty. | Nothing reaches `main` except through a pull request, as `CLAUDE.md` requires, and its history can't be rewritten. With 0 approvals the owner can still merge their own pull requests. The empty bypass list holds the owner to it too; in an emergency, set the ruleset to Disabled for a moment. |

## Not done

- **The owner's name in 28 commits.** Removing it would mean rewriting the
  whole history and force-pushing, which breaks every existing link and
  copy. It is the owner's own name, so it was left.
- **Old branches on GitHub.** The 25 merged `feature/` and `fix/` branches
  are still there. Every commit on them is already in `main`, so they show
  nothing new; deleting them is tidying, for the owner to decide.
- **A licence.** Without one, the code is public to read but all rights
  stay reserved. Choosing one is the owner's decision.
