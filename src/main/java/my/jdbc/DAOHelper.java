package my.jdbc;

import java.util.Iterator;

public final class DAOHelper {

	/**
	 * 
	 * Check if passed entity exists. If so, the entity is updated otherwise the
	 * entity is inserted. The method ALWAYS set the primary of the passed
	 * entity.
	 * 
	 * @param dao
	 * @param entity
	 * 
	 */
	public static <PK, T extends Entity<PK>> void upsert(DAOWithUpsertSupport<T, PK> dao, T entity) {
		PK pk;

		Iterator<PK> pkIterator = dao.getPrimaryKey(entity);
		if (pkIterator.hasNext()) {
			pk = pkIterator.next();
			dao.update(entity);
		} else {
			pk = dao.insert(entity);
		}

		entity.setPrimaryKey(pk);
	}

	public static <T, C> boolean exists(DAOWithExistenceSupport<T, C> dao, C searchCriterias) {
		return dao.search(searchCriterias).hasNext();
	}
}
