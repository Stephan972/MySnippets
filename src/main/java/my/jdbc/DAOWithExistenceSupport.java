package my.jdbc;

import java.util.Iterator;

public interface DAOWithExistenceSupport<T, C> {
	Iterator<T> search(C searchCriterias);
}
