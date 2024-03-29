import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;

/**
 * Handles database connection initiation and cleanup.
 * Implements {@link Callable} to be run asynchronously.
 *
 * Feel free to use this class in projects with proper attribution.
 *
 * @author Jennifer Teissler
 */
public abstract class CallableStatement<T> implements Callable<T> {

    private String query;
    private DataSource db;

    protected Connection connection;
    protected PreparedStatement statement;
    protected ResultSet result;

    /**
     * To be called as "super" from extending class.
     *
     * @param db    How to connect to the database
     * @param query SQL string to execute
     */
    protected CallableStatement(DataSource db, String query) {
        this.query = query;
        this.db = db;
    }

    /** Where to put your SQL code */
    protected abstract T query() throws SQLException;

    /**
     * Called automatically in the case of asynchronous execution.
     * Directly Call this function to execute synchronously.
     *
     * @return The results of {@link CallableStatement#query()}
     * @throws SQLException if there is an error connecting to the database
     *                      or in the query code.
     */
    public T call() throws SQLException {
        try {
            connection = db.getConnection();

            if (connection != null) {
                return query();
            } else {
                throw new SQLException("Could not connect to database");
            }
        } finally {
            if (connection != null) connection.close();
            if (statement != null) statement.close();
            if (result != null) result.close();
        }
    }

    /**
     * Call before executing your query.
     *
     * This method is to be used when auto-generated keys are wanted back
     * from the server. For example: inserting users into a table where
     * the users are automatically assigned an integer primary key to
     * identify them; using this function with keygen=true would allow
     * those unique IDs to be returned without the need for a second query.
     *
     * EXAMPLE: Code to return the autogenerated primary key of a created user.
     * <code>
     *     protected Integer query() throws SQLException {
     *         prepareStatement(true);
     *         statement.setString(1, "USERNAME");
     *         statement.setString(2, "PASSWORD");
     *         statement.executeUpdate();
     *         result = statement.getGeneratedKeys();
     *
     *         if (result.next()) {
     *             return result.getInt(1);
     *         }
     *     }
     * </code>
     *
     * @param keygen set to true to return auto-generated IDs
     * @throws SQLException if the statement has already been prepared.
     */
    protected void prepareStatement(boolean keygen) throws SQLException {
        if (statement != null && !statement.isClosed()) {
            throw new SQLException("Statement is already prepared");
        }

        statement = connection.prepareStatement(query,
                keygen ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
    }

    /**
     * Call before executing your query.
     * Prepares the sql statement passed into this class in the constructor.
     *
     * @throws SQLException if the statement has already been prepared.
     */
    protected void prepareStatement() throws SQLException {
        prepareStatement(false);
    }
}

