package uk.gov.justice.laa.analysis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class JdbcNullableDereferenceCases {

    /**
     * UNSAFE
     * getTimestamp() may return null if the column value is SQL NULL,
     * and calling toInstant() on a null reference will throw a NullPointerException.
     */
    public Instant unsafeTimestampColumnName(ResultSet rs) throws SQLException {
        return rs.getTimestamp("latest_end_time").toInstant();
    }

    /**
     * UNSAFE
     * getTimestamp() may return null if the column value is SQL NULL,
     * and calling toInstant() on a null reference will throw a NullPointerException.
     *
     * @param rs - the ResultSet from which to retrieve the timestamp
     * @return Instant representation of the timestamp, or null if the column value is SQL NULL
     *
     * @throws SQLException - if a database access error occurs or this method is called on a closed result set
     */
    public Instant unsafeTimestampColumnIndex(ResultSet rs) throws SQLException {
        return rs.getTimestamp(1).toInstant();
    }

    /**
     * UNSAFE
     * getString() may return null if the column value is SQL NULL,
     * and calling toString() on a null reference will throw a NullPointerException.
     *
     * @param rs - the ResultSet from which to retrieve the string
     * @return String representation of the value, or null if the column value is SQL NULL
     * @throws SQLException - if a database access error occurs or this method is called on a closed result set
     */
    public String unsafeStringTrim(ResultSet rs) throws SQLException {
        return rs.getString("name").trim();
    }

    /**
     * SAFE
     * The getTimestamp() method is called and the result is returned before dereferencing.
     * Direct dereference detector should not report this.
     *
     * @param rs - the ResultSet from which to retrieve the timestamp
     * @return Timestamp - representation of the value, or null if the column value is SQL NULL
     * @throws SQLException - if a database access error occurs or this method is called on a closed result set
     */
    public Instant safeTimestampNullCheck(ResultSet rs) throws SQLException {
        Timestamp timestamp =  rs.getTimestamp("latest_end_time");
        return timestamp == null ? null : timestamp.toInstant();
    }

    /**
     * SAFE
     * Nullable JDBC result is not itself is not a dereferenced.
     *
     * @param rs - the ResultSet from which to retrieve the timestamp
     * @return Timestamp - representation of the value, or null if the column value is SQL NULL
     * @throws SQLException - if a database access error occurs or this method is called on a closed result set
     */
    public Timestamp safeTimestampReturn(ResultSet rs) throws SQLException {
        return rs.getTimestamp("latest_end_time");
    }

    /**
     * SAFE
     * getString() is stored and checked before trim() is called.
     *
     * @param rs - the ResultSet from which to retrieve the string
     * @return String - representation of the value, or null if the column value is SQL NULL
     * @throws SQLException - if a database access error occurs or this method is called on a closed result set
     */
    public String safeStringNullCheck(ResultSet rs) throws SQLException {
        String value = rs.getString("name");
        return value == null ? null : value.trim();
    }

    /**
     * SAFE
     * Primitives returned from ResultSet getters are guaranteed to return a value, so this is safe.
     *
     * @param rs - the ResultSet from which to retrieve the integer
     * @return int - the integer value from the ResultSet
     * @throws SQLException - if a database access error occurs or this method is called on a closed result set
     */
    public int safePrimitiveGetter(ResultSet rs) throws SQLException {
        return rs.getInt("count");
    }
}
