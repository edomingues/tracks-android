package ca.xvx.tracks.persistence;

import java.util.Collections;
import java.util.List;

import ca.xvx.tracks.Task;


public interface ITaskDao {
	
	public void save(Task task);
	
	public Task getById(int id);
	
	public List<Task> getAll();

	public List<Long> deletedFromServer(List<Integer> taskIdsOnServer);
	
	public void delete(long id);
	
	public void deleteAll();
	
	public static final ITaskDao NULL = new ITaskDao() {

		@Override
		public void save(Task task) {
		}

		public Task getById(int id) {
			throw new IllegalStateException("NullObject DAO method invoked.");
		}
		
		@Override
		public List<Task> getAll() {
			return Collections.emptyList();			
		}
		
		public List<Long> deletedFromServer(List<Integer> taskIdsOnServer) {
			return Collections.emptyList();
		}

		public void delete(long id) {			
		}
		
		@Override
		public void deleteAll() {
		}
		
	};
}
