package my.jdbc;

public interface Entity<PK> {
	public PK getPrimaryKey();
	public void setPrimaryKey(PK pk);
}
