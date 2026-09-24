/**
 * Repository contracts for durable storage of Gymmie's domain records.
 *
 * <p>Every operation uses only its supplied, non-null, open {@link java.sql.Connection}.
 * Implementations must not open another connection, depend on a connection factory or
 * {@link gymmie.persistence.UnitOfWork}, or commit, roll back, close the connection, or change its
 * auto-commit setting. They close only their own statements and result sets. Nested collaborators
 * receive that same connection; no connection-opening overloads or convenience methods are permitted.
 *
 * <p>The application service owns the transaction through {@link gymmie.persistence.UnitOfWork}.
 * A successful write participates in that transaction; it becomes durable after the caller commits
 * to the file-backed database. Implementations must write to the supplied database, not merely cache
 * records in memory. Reads on that connection must reflect its uncommitted writes. Cross-record
 * changes, including cancellation cascades, must share a transaction.
 *
 * <p>Insert operations use the record's caller-assigned positive identifier and fail on duplicates.
 * Update operations require an existing identifier and fail rather than inserting missing records.
 * Neither operation may use delete-and-reinsert semantics that destroy historical references.
 * SQL failures, uniqueness violations and missing update targets are reported as
 * {@link java.sql.SQLException} so the transaction owner can roll back the complete operation.
 *
 * <p>Identifier lookups include inactive, archived, cancelled and expired records. List results are
 * immutable snapshots ordered by ascending identifier, empty when no records match, and never null.
 * Only explicitly filtered queries omit history. Stored fields must round-trip without loss,
 * including password hashes and salts, local dates and times, integer cents, snapshots and
 * cancellation reasons. Services enforce authorization and business transitions before writing.
 */
package gymmie.persistence.repository;
