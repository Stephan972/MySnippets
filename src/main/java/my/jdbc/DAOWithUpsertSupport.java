package my.jdbc;

import java.util.Iterator;

public interface DAOWithUpsertSupport<T, PK> {
	PK insert(T entity);

	void update(T entity);
	
	Iterator<PK> getPrimaryKey(T entity);
}
