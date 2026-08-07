# Changelog

## [1.5.2](https://github.com/ministryofjustice/laa-data-claims-reporting-service/compare/v1.5.1...v1.5.2) (2026-08-07)


### Bug Fixes

* **LPF-000:** fix rep002 dupes ([#236](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/236)) ([7b8833a](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/7b8833a0d5236d1c15b313e75f5c3ee5b06d9725))

## [1.5.1](https://github.com/ministryofjustice/laa-data-claims-reporting-service/compare/v1.5.0...v1.5.1) (2026-08-05)


### Bug Fixes

* **bot:** Bump the gradle-updates group with 6 updates ([#233](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/233)) ([d78afb5](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/d78afb596b8eeb9feff3533008848669251eed53))

## [1.5.0](https://github.com/ministryofjustice/laa-data-claims-reporting-service/compare/v1.4.0...v1.5.0) (2026-07-31)


### Features

* **LPF-1492:** REP000 support claim amendments in extract ([#230](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/230)) ([ade38b4](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/ade38b4bfc588d64f1c72890ccb3f6ea36a24e5d))


### Bug Fixes

* **LPF-1494:** handle multiple CFDs in REP013 ([#231](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/231)) ([fa80feb](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/fa80febcb3d97908c070e3d6237b523f5ce481d8))

## [1.4.0](https://github.com/ministryofjustice/laa-data-claims-reporting-service/compare/v1.3.0...v1.4.0) (2026-07-28)


### Features

* **LPF-1493:** Fix aggregation logic REP012 ([#225](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/225)) ([9438b15](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/9438b157702bd0af8489fb8cd263b45bf6bd7c97))


### Bug Fixes

* **bot:** Bump the gradle-updates group with 3 updates ([#228](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/228)) ([8760d83](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/8760d83091ab5f6aeb9a92119e0773cab1986abd))
* **bot:** Bump the minor group with 2 updates ([#229](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/229)) ([c956c2a](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/c956c2acb6dc7fd97ac2c9eb1201dcd3fea028ec))
* **LPF-1538:** Harden container defaults ([#223](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/223)) ([8c3e905](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/8c3e9054e34a595fec68f8ca07176f080a225655))

## [1.3.0](https://github.com/ministryofjustice/laa-data-claims-reporting-service/compare/v1.2.0...v1.3.0) (2026-07-15)


### Features

* **LPF-1530:** Create REP002 ([#215](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/215)) ([3446718](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/34467187609428d8b93d430b3c06fa4ee4ae2efd))

## [1.2.0](https://github.com/ministryofjustice/laa-data-claims-reporting-service/compare/v1.1.0...v1.2.0) (2026-07-10)


### Features

* **LPF-1527:** Replace Is Void with Claim Status ([#209](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/209)) ([ecf46a9](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/ecf46a95fc36bed33954fe9b609535d4c2998c00))


### Bug Fixes

* **LPF-0000:** Fix snyk issues ([#213](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/213)) ([09dd89f](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/09dd89fc7e9df0ddb506c4a67dcc98abeed0d58b))

## [1.1.0](https://github.com/ministryofjustice/laa-data-claims-reporting-service/compare/v1.0.0...v1.1.0) (2026-07-02)


### Features

* **LPF-1289:** add claim status to REP000 ([#186](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/186)) ([a992697](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/a992697842186051420755789fb4e80127752d8a))
* **LPF-1453:** add database metrics for Postgres at end of job ([#199](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/199)) ([1c16d3f](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/1c16d3f586ed58e684022a58c45d2afb681b197b))
* **LPF-1455:** Column changes for calculated fee details ([#187](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/187)) ([e420c31](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/e420c3103d6eefa3b9d59614bbff76e57a493c2c))
* **LPF-1475:** allow NULL bulk submission ids to align with Claims DB change ([#198](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/198)) ([d0c3f40](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/d0c3f405b6fd6a5c4d1800fc41a6da134cf991f8))
* **LPF-1491:** Add missing fields REP000, reorder fields ([#204](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/204)) ([ec59843](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/ec598434085f46637163ea57ca6c1eadab2bc0d6))


### Bug Fixes

* **LPF-1367:** Revert changes in Grafana dashboard ([#185](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/185)) ([85be489](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/85be4896e5e338cd32c867a37195817b93cf23e3))

## 1.0.0 (2026-06-01)


### Features

* **LPF-1122:** implement thanos ([#169](https://github.com/ministryofjustice/laa-data-claims-reporting-service/pull/169)) ([19d8b0e](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/19d8b0e51e7f58ac603be0cb8308f57bf71a285d))
* **LPF-1378:** Implement automated version management ([#168](https://github.com/ministryofjustice/laa-data-claims-reporting-service/pull/168)) ([b8d64fc](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/b8d64fcca8b770bd6d1f1ffbb30b0930f5e43ecd))
* **LPF-1398:** add mental health tribunal reference to REP000 ([#176](https://github.com/ministryofjustice/laa-data-claims-reporting-service/pull/176) ([ebe35ca](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/ebe35ca7aa0fcd9334a08529a6dbcf534ca82eb5))


### Bug Fixes

* **LPF-1378:** remove commit message enforcement ([#172](https://github.com/ministryofjustice/laa-data-claims-reporting-service/issues/172)) ([67678b0](https://github.com/ministryofjustice/laa-data-claims-reporting-service/commit/67678b08f4325a987ba390521171a5cf33fe9693))
