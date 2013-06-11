package ca.xvx.tracks;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import org.apache.http.HttpResponse;
import org.xml.sax.SAXException;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import ca.xvx.tracks.preferences.PreferenceConstants;
import ca.xvx.tracks.preferences.PreferenceUtils;
import ca.xvx.tracks.util.HttpConnection;

public class TracksCommunicator extends HandlerThread {
	private static final String TAG = "TracksCommunicator";
	
	private static Handler _handler;
	private static SharedPreferences _prefs;
	private static Semaphore _ready;

	public static final int SUCCESS_CODE = 0;
	public static final int FETCH_CODE = 1;
	public static final int PARSE_CODE = 2;
	public static final int PARSE_FAIL_CODE = 3;
	public static final int FETCH_FAIL_CODE = 4;
	public static final int PREFS_FAIL_CODE = 5;
	public static final int UPDATE_FAIL_CODE = 6;

	private static final SimpleDateFormat DATEFORM = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public TracksCommunicator(SharedPreferences prefs) {
		super("Tracks Communicator");
		_prefs = prefs;
		_ready = new Semaphore(1);
		_ready.acquireUninterruptibly();
	}

	@Override
	protected void onLooperPrepared() {
		_handler = new CommHandler();
		_ready.release();
		Log.v(TAG, "Ready");
	}

	public static Handler getHandler() {
		if(_handler == null) {
			_ready.acquireUninterruptibly();
			_ready.release();
		}
		
		return _handler;
	}

	private void fetchTasks(TracksAction act) {
		final String server = _prefs.getString(PreferenceConstants.SERVER, null);
		final boolean badcert = _prefs.getBoolean(PreferenceConstants.BADCERT, false);
		final String username = _prefs.getString(PreferenceConstants.USERNAME, null);
		final String password = _prefs.getString(PreferenceConstants.PASSWORD, null);

		Log.d(TAG, "Fetching tasks");

		Handler replyTo = act.notify;
		
		if(server == null || username == null || password == null) {
			Message.obtain(replyTo, PREFS_FAIL_CODE).sendToTarget();
			return;
		}
		
		HttpResponse r;
		InputStream[] ret = new InputStream[3];

		Message.obtain(replyTo, FETCH_CODE).sendToTarget();

		try {
			r = HttpConnection.get(PreferenceUtils.getUri(_prefs, "contexts.xml"), username, password, badcert);
			ret[0] = r.getEntity().getContent();
			
			r = HttpConnection.get(PreferenceUtils.getUri(_prefs, "projects.xml"), username, password, badcert);
			ret[1] = r.getEntity().getContent();
			
			r = HttpConnection.get(PreferenceUtils.getUri(_prefs, "todos.xml"), username, password, badcert);
			ret[2] = r.getEntity().getContent();
		} catch(Exception e) {
			Log.w(TAG, "Failed to fetch tasks!", e);
			Message.obtain(replyTo, FETCH_FAIL_CODE).sendToTarget();
			return;
		}

		Message.obtain(replyTo, PARSE_CODE).sendToTarget();
		
		try {						
			Xml.parse(ret[0], Xml.Encoding.UTF_8, new ContextXmlHandler());
			Xml.parse(ret[1], Xml.Encoding.UTF_8, new ProjectXmlHandler());
			Xml.parse(ret[2], Xml.Encoding.UTF_8, new TaskXmlHandler());
		} catch(IOException e) {
			Log.w(TAG, "Failed to read XML!", e);
			Message.obtain(replyTo, FETCH_FAIL_CODE).sendToTarget();
			return;
		} catch(SAXException e) {
			Log.w(TAG, "Failed to parse XML!", e);
			Message.obtain(replyTo, PARSE_FAIL_CODE).sendToTarget();
			return;
		}
		
		Message.obtain(replyTo, SUCCESS_CODE).sendToTarget();
	}

	private void completeTask(TracksAction act) {
		Task t = (Task)act.target;

		Log.d(TAG, "Marking task " + String.valueOf(t.getId()) + " as done");

		boolean failed = false;
		try {
			completeTask(t);
		} catch(Exception e) {
			Log.w(TAG, "Exception caught while marking task as done on server.", e);
			failed = true;
		}
		
		if(failed) {
			t.setDone(true);
			t.setOutdated(true);
			Task.save(t);
		}
		else {
			t.remove();
		}		
		act.notify.sendEmptyMessage(0);
	}
	
	private void completeTask(Task task) throws URISyntaxException, Exception {
		final boolean badcert = _prefs.getBoolean(PreferenceConstants.BADCERT, false);
		final String username = _prefs.getString(PreferenceConstants.USERNAME, null);
		final String password = _prefs.getString(PreferenceConstants.PASSWORD, null);
		
		HttpConnection.put(PreferenceUtils.getUri(_prefs, "todos/" +
				 String.valueOf(task.getId()) + "/toggle_check.xml"),
				 username,
				 password,
				 null, badcert);
	}

	private void deleteTask(TracksAction act) {
		Task t = (Task)act.target;

		Log.d(TAG, "Deleting task " + String.valueOf(t.getId()));

		boolean failed = false;
		try {
			deleteTask(t);
		} catch(Exception e) {			
			failed = true;
		}
		
		if(failed) {
			t.setDeleted(true);
			t.setOutdated(true);
			Task.save(t);
		} else {
			t.remove();
		}
		act.notify.sendEmptyMessage(0);
	}
	
	private void deleteTask(Task task) throws URISyntaxException, Exception {
		final boolean badcert = _prefs.getBoolean(PreferenceConstants.BADCERT, false);
		final String username = _prefs.getString(PreferenceConstants.USERNAME, null);
		final String password = _prefs.getString(PreferenceConstants.PASSWORD, null);
		
		HttpConnection.delete(PreferenceUtils.getUri(_prefs, "todos/" +
				String.valueOf(task.getId()) + ".xml"),
					username,
					password, badcert);
	}

	private void updateTask(TracksAction act) {
		final boolean badcert = _prefs.getBoolean(PreferenceConstants.BADCERT, false);
		final String username = _prefs.getString(PreferenceConstants.USERNAME, null);
		final String password = _prefs.getString(PreferenceConstants.PASSWORD, null);

		Task t = (Task)act.target;

		Log.d(TAG, "Updating task " + String.valueOf(t.getId()));

		StringBuilder xml = new StringBuilder("<todo>");
		xml.append("<description>"); xml.append(t.getDescription()); xml.append("</description>");
		xml.append("<notes>"); xml.append(t.getNotes() == null ? "" : t.getNotes()); xml.append("</notes>");
		xml.append("<context-id type=\"integer\">");
		xml.append(String.valueOf(t.getContext().getId())); xml.append("</context-id>");
		
		xml.append("<project-id type=\"integer\"");
		if(t.getProject() == null) {
			xml.append(" nil=\"true\"></project-id>");
		} else {
			xml.append(">"); xml.append(String.valueOf(t.getProject().getId())); xml.append("</project-id>");
		}
		
		xml.append("<due type=\"datetime\"");
		if(t.getDue() == null) {
			xml.append(" nil=\"true\"></due>");
		} else {
			xml.append(">");
			xml.append(DATEFORM.format(t.getDue()));
			xml.append("</due>");
		}
		
		xml.append("<show-from type=\"datetime\"");
		if(t.getShowFrom() == null) {
			xml.append(" nil=\"true\"></show-from>");
		} else {
			xml.append(">");
			xml.append(DATEFORM.format(t.getShowFrom()));
			xml.append("</show-from>");
		}

		xml.append("</todo>");

		Log.v(TAG, "Sending: " + xml.toString());

		try {
			HttpResponse r;
			int resp;

			if(t.getId() < 0) {
				Log.v(TAG, "Posting to todos.xml to create new task");
				r = HttpConnection.post(PreferenceUtils.getUri(_prefs, "todos.xml"), username, password,
										xml.toString(), badcert);
			} else {
				Log.v(TAG, "Putting to update existing task");
				r = HttpConnection.put(PreferenceUtils.getUri(_prefs,
														 "todos/" + String.valueOf(t.getId()) + ".xml"),
									   username, password, xml.toString(), badcert);
			}

			resp = r.getStatusLine().getStatusCode();

			if(resp == 200) {
				Log.d(TAG, "Successfully updated task");
				act.notify.sendEmptyMessage(SUCCESS_CODE);
			} else if(resp == 201) {
				Log.d(TAG, "Successfully created task.");
				String got = r.getFirstHeader("Location").getValue();
				got = got.substring(got.lastIndexOf('/') + 1);
				int tno = Integer.parseInt(got);
				t.setId(tno);
				Log.d(TAG, "ID of new task is: " + String.valueOf(tno));
				act.notify.sendEmptyMessage(SUCCESS_CODE);
			} else {
				Log.w(TAG, "Unexpected response from server: " + String.valueOf(resp));
				act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
			}
		} catch(Exception e) {
			Log.w(TAG, "Error updating task", e);
			act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
		}
	}

	private void updateContext(TracksAction act) {
		final boolean badcert = _prefs.getBoolean(PreferenceConstants.BADCERT, false);
		final String username = _prefs.getString(PreferenceConstants.USERNAME, null);
		final String password = _prefs.getString(PreferenceConstants.PASSWORD, null);

		TodoContext c = (TodoContext)act.target;

		Log.d(TAG, "Updating context " + String.valueOf(c.getId()));

		StringBuilder xml = new StringBuilder("<context>");
		xml.append("<name>"); xml.append(c.getName()); xml.append("</name>");
		xml.append("<hide type=\"boolean\">"); xml.append(c.isHidden() ? "true" : "false"); xml.append("</hide>");
		xml.append("<position type=\"integer\">"); xml.append(String.valueOf(c.getPosition())); xml.append("</position>");
		xml.append("</context>");

		Log.v(TAG, "Sending: " + xml.toString());

		try {
			HttpResponse r;
			int resp;

			if(c.getId() < 0) {
				Log.v(TAG, "Posting to contexts.xml to create new context");
				r = HttpConnection.post(PreferenceUtils.getUri(_prefs, "contexts.xml"), username, password,
										xml.toString(), badcert);
			} else {
				Log.v(TAG, "Putting to update existing context");
				r = HttpConnection.put(PreferenceUtils.getUri(_prefs,
														 "contexts/" + String.valueOf(c.getId()) + ".xml"),
									   username, password, xml.toString(), badcert);
			}

			resp = r.getStatusLine().getStatusCode();

			if(resp == 200) {
				Log.d(TAG, "Successfully updated context");
				act.notify.sendEmptyMessage(SUCCESS_CODE);
			} else if(resp == 201) {
				Log.d(TAG, "Successfully created context.");
				String got = r.getFirstHeader("Location").getValue();
				got = got.substring(got.lastIndexOf('/') + 1);
				int cno = Integer.parseInt(got);
				c.setId(cno);
				Log.d(TAG, "ID of new context is: " + String.valueOf(cno));
				act.notify.sendEmptyMessage(SUCCESS_CODE);
			} else {
				Log.w(TAG, "Unexpected response from server: " + String.valueOf(resp));
				act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
			}
		} catch(Exception e) {
			Log.w(TAG, "Error updating context", e);
			act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
		}		
	}

	private void updateProject(TracksAction act) {
		final boolean badcert = _prefs.getBoolean(PreferenceConstants.BADCERT, false);
		final String username = _prefs.getString(PreferenceConstants.USERNAME, null);
		final String password = _prefs.getString(PreferenceConstants.PASSWORD, null);

		Project p = (Project)act.target;

		Log.d(TAG, "Updating project " + String.valueOf(p.getId()));

		StringBuilder xml = new StringBuilder("<project>");
		xml.append("<name>"); xml.append(p.getName()); xml.append("</name>");
		xml.append("<description>"); xml.append(p.getDescription() == null ? "" : p.getDescription()); xml.append("</description>");
		xml.append("</project>");

		Log.v(TAG, "Sending: " + xml.toString());

		try {
			HttpResponse r;
			int resp;

			if(p.getId() < 0) {
				Log.v(TAG, "Posting to contexts.xml to create new context");
				r = HttpConnection.post(PreferenceUtils.getUri(_prefs, "projects.xml"), username, password,
										xml.toString(), badcert);
			} else {
				Log.v(TAG, "Putting to update existing context");
				r = HttpConnection.put(PreferenceUtils.getUri(_prefs,
														 "projects/" + String.valueOf(p.getId()) + ".xml"),
									   username, password, xml.toString(), badcert);
			}

			resp = r.getStatusLine().getStatusCode();

			if(resp == 200) {
				Log.d(TAG, "Successfully updated context");
				act.notify.sendEmptyMessage(SUCCESS_CODE);
			} else if(resp == 201) {
				Log.d(TAG, "Successfully created context.");
				String got = r.getFirstHeader("Location").getValue();
				got = got.substring(got.lastIndexOf('/') + 1);
				int pno = Integer.parseInt(got);
				p.setId(pno);
				Log.d(TAG, "ID of new project is: " + String.valueOf(pno));
				act.notify.sendEmptyMessage(SUCCESS_CODE);
			} else {
				Log.w(TAG, "Unexpected response from server: " + String.valueOf(resp));
				act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
			}
		} catch(Exception e) {
			Log.w(TAG, "Error updating context", e);
			act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
		}		
	}
	
	private void uploadTasks(TracksAction act) {
		final String server = _prefs.getString(PreferenceConstants.SERVER, null);
		final boolean badcert = _prefs.getBoolean(PreferenceConstants.BADCERT, false);
		final String username = _prefs.getString(PreferenceConstants.USERNAME, null);
		final String password = _prefs.getString(PreferenceConstants.PASSWORD, null);

		Log.d(TAG, "Uploading tasks");

		Handler replyTo = act.notify;
		
		if(server == null || username == null || password == null) {
			Message.obtain(replyTo, PREFS_FAIL_CODE).sendToTarget();
			return;
		}
		
		boolean error = false;
		Iterator<TodoContext> itContexts = TodoContext.getAllContexts().iterator();
		
		while(itContexts.hasNext() && !error) {
			TodoContext c = itContexts.next();
			if(c.isOutdated()) {
				Log.d(TAG, "Updating context " + String.valueOf(c.getId()));

				StringBuilder xml = new StringBuilder("<context>");
				xml.append("<name>"); xml.append(c.getName()); xml.append("</name>");
				xml.append("<hide type=\"boolean\">"); xml.append(c.isHidden() ? "true" : "false"); xml.append("</hide>");
				xml.append("<position type=\"integer\">"); xml.append(String.valueOf(c.getPosition())); xml.append("</position>");
				xml.append("</context>");

				Log.v(TAG, "Sending: " + xml.toString());

				try {
					HttpResponse r;
					int resp;

					if(c.getId() < 0) {
						Log.v(TAG, "Posting to contexts.xml to create new context");
						r = HttpConnection.post(PreferenceUtils.getUri(_prefs, "contexts.xml"), username, password,
												xml.toString(), badcert);
					} else {
						Log.v(TAG, "Putting to update existing context");
						r = HttpConnection.put(PreferenceUtils.getUri(_prefs,
																 "contexts/" + String.valueOf(c.getId()) + ".xml"),
											   username, password, xml.toString(), badcert);
					}

					resp = r.getStatusLine().getStatusCode();

					if(resp == 200) {
						Log.d(TAG, "Successfully updated context");
//						act.notify.sendEmptyMessage(SUCCESS_CODE);
					} else if(resp == 201) {
						Log.d(TAG, "Successfully created context.");
						String got = r.getFirstHeader("Location").getValue();
						got = got.substring(got.lastIndexOf('/') + 1);
						int cno = Integer.parseInt(got);
						c.setId(cno);
						Log.d(TAG, "ID of new context is: " + String.valueOf(cno));
//						act.notify.sendEmptyMessage(SUCCESS_CODE);						
					} else {
						Log.w(TAG, "Unexpected response from server: " + String.valueOf(resp));
//						act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
						error = true;
					}
				} catch(Exception e) {
					Log.w(TAG, "Error updating context", e);
//					act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
					error = true;
				}	
				if(!error) {
					c.setOutdated(false);
					TodoContext.saveServerContext(c);
				}
			}
		}
		
		Iterator<Project> itProjects = Project.getAllProjects().iterator();
		while(itProjects.hasNext() && !error) {
			Project p = itProjects.next();
			if(p.isOutdated()) {
				Log.d(TAG, "Updating project " + String.valueOf(p.getId()));

				StringBuilder xml = new StringBuilder("<project>");
				xml.append("<name>"); xml.append(p.getName()); xml.append("</name>");
				xml.append("<description>"); xml.append(p.getDescription() == null ? "" : p.getDescription()); xml.append("</description>");
				xml.append("</project>");

				Log.v(TAG, "Sending: " + xml.toString());

				try {
					HttpResponse r;
					int resp;

					if(p.getId() < 0) {
						Log.v(TAG, "Posting to projects.xml to create new project");
						r = HttpConnection.post(PreferenceUtils.getUri(_prefs, "projects.xml"), username, password,
												xml.toString(), badcert);
					} else {
						Log.v(TAG, "Putting to update existing project");
						r = HttpConnection.put(PreferenceUtils.getUri(_prefs,
																 "projects/" + String.valueOf(p.getId()) + ".xml"),
											   username, password, xml.toString(), badcert);
					}

					resp = r.getStatusLine().getStatusCode();

					if(resp == 200) {
						Log.d(TAG, "Successfully updated project");
						//act.notify.sendEmptyMessage(SUCCESS_CODE);
						p.setOutdated(false);
					} else if(resp == 201) {
						Log.d(TAG, "Successfully created project.");
						String got = r.getFirstHeader("Location").getValue();
						got = got.substring(got.lastIndexOf('/') + 1);
						int pno = Integer.parseInt(got);
						p.setId(pno);
						Log.d(TAG, "ID of new project is: " + String.valueOf(pno));
//						act.notify.sendEmptyMessage(SUCCESS_CODE);
						p.setOutdated(false);
					} else {
						Log.w(TAG, "Unexpected response from server: " + String.valueOf(resp));
//						act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
						error = true;
					}
				} catch(Exception e) {
					Log.e(TAG, "Error updating project", e);
//					act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
					error = true;
				}	
			}
		}
		
		Iterator<Task> itTasks = new ArrayList<Task>(Task.getAllTasks()).iterator();
		while(itTasks.hasNext() && !error) {
			Task t = itTasks.next();
			if(t.isDeleted()) {
				try {
					deleteTask(t);	
					t.remove();
				} catch (Exception e) {
					Log.e(TAG, "Error deleting task", e);
					error = true;
				}
			} else if(t.isOutdated()) {
				Log.d(TAG, "Updating task " + String.valueOf(t.getId()));

				StringBuilder xml = new StringBuilder("<todo>");
				xml.append("<description>"); xml.append(t.getDescription()); xml.append("</description>");
				xml.append("<notes>"); xml.append(t.getNotes() == null ? "" : t.getNotes()); xml.append("</notes>");
				xml.append("<context-id type=\"integer\">");
				xml.append(String.valueOf(t.getContext().getId())); xml.append("</context-id>");
				
				xml.append("<project-id type=\"integer\"");
				if(t.getProject() == null) {
					xml.append(" nil=\"true\"></project-id>");
				} else {
					xml.append(">"); xml.append(String.valueOf(t.getProject().getId())); xml.append("</project-id>");
				}
				
				xml.append("<due type=\"datetime\"");
				if(t.getDue() == null) {
					xml.append(" nil=\"true\"></due>");
				} else {
					xml.append(">");
					xml.append(DATEFORM.format(t.getDue()));
					xml.append("</due>");
				}
				
				xml.append("<show-from type=\"datetime\"");
				if(t.getShowFrom() == null) {
					xml.append(" nil=\"true\"></show-from>");
				} else {
					xml.append(">");
					xml.append(DATEFORM.format(t.getShowFrom()));
					xml.append("</show-from>");
				}
				
				if(t.getDone()) {
					xml.append("<state>completed</state>");
					if(t.completedAt() != null)
						xml.append("<completed-at type=\"datetime\">")
						   .append(DATEFORM.format(t.completedAt()))
						   .append("</completed-at>");
				}
				

				xml.append("</todo>");

				Log.v(TAG, "Sending: " + xml.toString());

				try {
					HttpResponse r;
					int resp;

					if(t.getId() < 0) {
						Log.v(TAG, "Posting to todos.xml to create new task");
						r = HttpConnection.post(PreferenceUtils.getUri(_prefs, "todos.xml"), username, password,
												xml.toString(), badcert);
					} else {
						Log.v(TAG, "Putting to update existing task");
						r = HttpConnection.put(PreferenceUtils.getUri(_prefs,
																 "todos/" + String.valueOf(t.getId()) + ".xml"),
											   username, password, xml.toString(), badcert);
					}

					resp = r.getStatusLine().getStatusCode();

					if(resp == 200) {
						Log.d(TAG, "Successfully updated task");
//						act.notify.sendEmptyMessage(SUCCESS_CODE);
					} else if(resp == 201) {
						Log.d(TAG, "Successfully created task.");
						String got = r.getFirstHeader("Location").getValue();
						got = got.substring(got.lastIndexOf('/') + 1);
						int tno = Integer.parseInt(got);
						t.setId(tno);
						Task.save(t);
						Log.d(TAG, "ID of new task is: " + String.valueOf(tno));
//						act.notify.sendEmptyMessage(SUCCESS_CODE);						
					} else {
						Log.w(TAG, "Unexpected response from server: " + String.valueOf(resp));
//						act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
						error = true;
					}
				} catch(Exception e) {
					Log.e(TAG, "Error updating task", e);
//					act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
					error = true;
				}			
				if(!error) {
					t.setOutdated(false);
					Task.saveServerTask(t);
				}
			}
		}
		
		if(error)
			act.notify.sendEmptyMessage(UPDATE_FAIL_CODE);
		else
			act.notify.sendEmptyMessage(SUCCESS_CODE);
	}

	private class CommHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			TracksAction act = (TracksAction)msg.obj;
			
			switch(act.type) {
			case FETCH_TASKS:
				fetchTasks(act);
				break;
				
			case COMPLETE_TASK:
				completeTask(act);
				break;

			case UPDATE_TASK:
				updateTask(act);
				break;

			case DELETE_TASK:
				deleteTask(act);
				break;

			case UPDATE_CONTEXT:
				updateContext(act);
				break;

			case UPDATE_PROJECT:
				updateProject(act);
				break;
				
			case UPLOAD_TASKS:
				uploadTasks(act);
				break;
			}
		}
	}
}