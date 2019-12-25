package my.jdbc.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class ExistsMapper implements ResultSetMapper<Boolean> {

	/**
	 * 
	 * Expect that the first column contains a number indicating if a given
	 * entity exists or not.
	 * 
	 * If first column equals 1 then the entity exists. If first column equals 0
	 * or any other value then the entity doesn't exist.
	 * 
	 * @param index
	 * @param rs
	 * @param ctx
	 * 
	 * @return TRUE => the entity exists FALSE => the entity doesn't exist
	 * 
	 * @see https://github.com/perspilling/jdbi-examples/blob/master/src/main/java/no/kodemaker/ps/jdbiapp/repository/mappers/ExistsMapper.java
	 */
	public Boolean map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
		//FIXME: Implementation of ExistsMapper#map : SELECT count(pk) *OR* SELECT NULL ??
// ** For certain the code below works while in other project it doesn't... wtf??
//		boolean ret = false;
//
//		if (rs.next()) {
//			ret = (rs.getInt(1) == 1);
//		}
//
//		return ret;
		return (rs.getInt(1) == 1);
	}
}
