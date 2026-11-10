# Contributing to Rimalux Desktop

Thanks for taking the time to contribute.

## Before you start

- Open an issue for bugs, feature ideas, or larger changes so the approach can be agreed first.
- Keep changes focused. Unrelated refactors make review harder.
- Read `AGENTS.md` (architecture and UI rules) and `RULES.md` (testing and visual verification).

## Development

```bash
./gradlew test
./gradlew :desktopApp:run
```

`./gradlew test` must pass before a pull request is opened. Do not launch the GUI for routine
verification without the maintainer present.

## License of contributions

Rimalux Desktop is licensed under the **PolyForm Perimeter License 1.0.1** (see `LICENSE`).
By contributing, you agree that your contribution is licensed under those same terms.

## Developer Certificate of Origin

Every commit must be signed off with the Developer Certificate of Origin 1.1. Add the sign-off
with:

```bash
git commit -s -m "your message"
```

This appends a line like:

```text
Signed-off-by: Your Name <you@example.com>
```

The sign-off certifies the following:

```text
Developer Certificate of Origin
Version 1.1

Copyright (C) 2004, 2006 The Linux Foundation and its contributors.

Everyone is permitted to copy and distribute verbatim copies of this
license document, but changing it is not allowed.


Developer's Certificate of Origin 1.1

By making a contribution to this project, I certify that:

(a) The contribution was created in whole or in part by me and I
    have the right to submit it under the open source license
    indicated in the file; or

(b) The contribution is based upon previous work that, to the best
    of my knowledge, is covered under an appropriate open source
    license and I have the right under that license to submit that
    work with modifications, whether created in whole or in part
    by me, under the same open source license (unless I am
    permitted to submit under a different license), as indicated
    in the file; or

(c) The contribution was provided directly to me by some other
    person who certified (a), (b) or (c) and I have not modified
    it.

(d) I understand and agree that this project and the contribution
    are public and that a record of the contribution (including all
    personal information I submit with it, including my sign-off) is
    maintained indefinitely and may be redistributed consistent with
    this project or the open source license(s) involved.
```

Pull requests without sign-off will be asked to amend their commits.

## Third-party code

Do not paste code from projects whose license is incompatible with PolyForm Perimeter 1.0.1.
If you add a dependency, update `THIRD_PARTY_NOTICES.md`.
