package my.jdbc;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.skife.jdbi.v2.DBI;

public class DAOFactory {
	private List<GenericDAO> openedDAOs;
	private DBI dbi;

	public DAOFactory(DataSource ds) {
		openedDAOs = new ArrayList<>();
		dbi = new DBI(ds);
	}

	public void freeAllOpenedDAOs() {
		int len = openedDAOs.size();
		while (len > 0) {
			closeSilently(openedDAOs.get(0));
			len--;
		}
	}

	public <T extends GenericDAO> T getDAO(Class<T> clazz) {
		T newDAO = dbi.open(clazz);
		openedDAOs.add(newDAO);
		return newDAO;
	}

	public void closeSilently(GenericDAO dao) {
		if (dao != null) {
			dao.close();
			openedDAOs.remove(dao);
		}
	}
}
