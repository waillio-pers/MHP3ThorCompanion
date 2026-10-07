# Release signing setup

Release signing reads these four values from Gradle properties first, then from
environment variables: `MHP3_RELEASE_STORE_FILE`,
`MHP3_RELEASE_STORE_PASSWORD`, `MHP3_RELEASE_KEY_ALIAS`, and
`MHP3_RELEASE_KEY_PASSWORD`. Keep the keystore and credential values outside
the repository; a user-level `%USERPROFILE%\.gradle\gradle.properties` or
process environment variables can supply them. Forward slashes work for
Windows keystore paths. `assembleRelease`, `packageRelease`, and `bundleRelease`
fail with a missing-configuration message unless all values are present and
the external keystore file exists. Release never falls back to debug signing.
