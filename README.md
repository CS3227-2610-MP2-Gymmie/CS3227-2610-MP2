# Gymmie

Gymmie is a JavaFX desktop app for gym management.

On launch (`./gradlew run`), Gymmie creates an active Manager account if the
case-insensitive username `manager` is absent:

- Username: `manager`
- Password: `manager123`

Seeding runs after schema initialization in a transaction against `data/gymmie.db`.
Other accounts, including inactive accounts, do not prevent seeding. The seed uses
the first available positive account ID and preserves existing accounts.
An existing active Manager with this username is left unchanged, including its
password. If the username belongs to another role or an inactive Manager, startup
fails with a clear conflict error instead of overwriting or reactivating it.

The seeded Manager is identified by its immutable username (case-insensitive)
and Manager role and cannot be deactivated. Its password can be changed through
the normal password-change service without losing that protection. The initial
password uses the application's password-storage rules: PBKDF2-HMAC-SHA256 with
600,000 iterations and a fresh random 128-bit salt. Only the hash and salt are
stored in the database, never the plaintext password.
