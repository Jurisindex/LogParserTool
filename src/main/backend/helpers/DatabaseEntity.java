package helpers;

import java.util.List;

public abstract class DatabaseEntity
{
	public DatabaseEntity() {}

	public DatabaseEntity(List<String> columns) {}

	public abstract List<String> toList();
}
