## What

[Link to ticket](https://dsdmoj.atlassian.net/browse/LPF-XXX)

Describe what you did and why.

## Checklist

Before you ask people to review this PR:

- [ ] Bump Helm chart version (if you have changed the Helm templates)
- [ ] Tests should be passing: `./gradlew test`
- [ ] Integration tests should be passing: `./gradlew integrationTest`
- [ ] Github should not be reporting conflicts; you should have recently run `git rebase main`.
- [ ] Avoid mixing whitespace changes with code changes in the same commit. These make diffs harder to read and conflicts more likely.
- [ ] You should have looked at the diff against main and ensured that nothing unexpected is included in your changes.
- [ ] You should have checked that the commit messages say why the change was made.
