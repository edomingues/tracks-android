package ca.xvx.tracks.persistence;

import java.util.Collections;
import java.util.List;

import ca.xvx.tracks.TodoContext;

public interface ITodoContextDao {

	public TodoContext load(long id);
	
	void save(TodoContext context);
	
	public List<Long> deletedFromServer(List<Integer> contextIdsOnServer);
	
	void delete(long id);

	void deleteAll();

	List<TodoContext> getAll();
	
	TodoContext getById(int id);
	
	public static final ITodoContextDao NULL = new ITodoContextDao() {

		@Override
		public void save(TodoContext context) {
		}
		
		public java.util.List<Long> deletedFromServer(java.util.List<Integer> contextIdsOnServer) {
			return Collections.emptyList();
		}
		
		@Override
		public
		void delete(long id) {			
		}

		@Override
		public void deleteAll() {
		}

		@Override
		public List<TodoContext> getAll() {
			return Collections.emptyList();
		}

		@Override
		public TodoContext load(long id) {
			throw new IllegalStateException("NullObject DAO method invoke.");
		}
		
		public TodoContext getById(int id) {
			throw new IllegalStateException("NullObject DAO method invoke.");
		};
		
	};

}
