package ca.xvx.tracks;

import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;
import ca.xvx.tracks.persistence.DatabaseManager;
import ca.xvx.tracks.preferences.PreferenceConstants;

public class TaskListActivity extends ExpandableListActivity {
	private static final String TAG = "TaskListActivity";
	
	private TaskListAdapter _tla;
	private SharedPreferences _prefs;
	private Handler _commHandler;

	private static final int SETTINGS = 1;
	private static final int NEW_TASK = 2;
	private static final int EDIT_TASK = 2;
	private static final int NEW_CONTEXT = 3;
	private static final int EDIT_CONTEXT = 3;
	private static final int NEW_PROJECT = 4;
	private static final int EDIT_PROJECT = 4;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.v(TAG, "Created!");
		
		_prefs = PreferenceManager.getDefaultSharedPreferences(this);

		TracksCommunicator comm = new TracksCommunicator(_prefs);
		comm.start();
		_commHandler = TracksCommunicator.getHandler();
		
		_tla = new TaskListAdapter();
		setListAdapter(_tla);
		registerForContextMenu(getExpandableListView());
		
		if(!_prefs.getBoolean(PreferenceConstants.RUN, false)) {
			Log.i(TAG, "This appears to be our first run; edit preferences");
			startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS);			
		} else {
			Log.v(TAG, "Fetching tasks");
			configurePersistence();
			loadList();
			refreshList();
		}
	}
	
	private void configurePersistence() {
		boolean persist = true;
		if(persist) {
			DatabaseManager.configurePersistence(getApplicationContext());			
		}
	}
	
	private void loadList() {		
		TodoContext.loadAllContexts();
		Project.loadAllProjects();
		Task.loadAllTasks();
		updateListView();
	}

	private void refreshList() {
		final Context context = getExpandableListView().getContext();
		final ProgressDialog p = ProgressDialog.show(context, "", "", true);
		TracksAction a = new TracksAction(TracksAction.ActionType.FETCH_TASKS, null, new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch(msg.what) {
					case TracksCommunicator.FETCH_CODE:
						p.setMessage(getString(R.string.MSG_fetching));
						break;
					case TracksCommunicator.PARSE_CODE:
						p.setMessage(getString(R.string.MSG_parsing));
						break;
					case TracksCommunicator.PREFS_FAIL_CODE:
						p.dismiss();
						Toast.makeText(context, getString(R.string.ERR_badprefs), Toast.LENGTH_LONG).show();
						break;
					case TracksCommunicator.PARSE_FAIL_CODE:
						p.dismiss();
						Toast.makeText(context, getString(R.string.ERR_parse), Toast.LENGTH_LONG).show();
						break;
					case TracksCommunicator.FETCH_FAIL_CODE:
						p.dismiss();
						Toast.makeText(context, getString(R.string.ERR_fetch), Toast.LENGTH_LONG).show();
						break;
					case TracksCommunicator.SUCCESS_CODE:						
//						p.dismiss();
						
						uploadList(p);
//						updateListView();
												
						break;
					}
				}
				
			});
		
		Message.obtain(_commHandler, 0, a).sendToTarget();
	}
	
	private void uploadList(final ProgressDialog p) {
		final Context context = getExpandableListView().getContext();
		p.setMessage(getString(R.string.MSG_uploading));
		TracksAction a = new TracksAction(TracksAction.ActionType.UPLOAD_TASKS, null, new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch(msg.what) {
//					case TracksCommunicator.FETCH_CODE:
//						p.setMessage(getString(R.string.MSG_fetching));
//						break;
//					case TracksCommunicator.PARSE_CODE:
//						p.setMessage(getString(R.string.MSG_parsing));
//						break;
					case TracksCommunicator.PREFS_FAIL_CODE:
						p.dismiss();
						Toast.makeText(context, getString(R.string.ERR_badprefs), Toast.LENGTH_LONG).show();
						break;
//					case TracksCommunicator.PARSE_FAIL_CODE:
//						p.dismiss();
//						Toast.makeText(context, getString(R.string.ERR_parse), Toast.LENGTH_LONG).show();
//						break;
//					case TracksCommunicator.FETCH_FAIL_CODE:
//						p.dismiss();
//						Toast.makeText(context, getString(R.string.ERR_fetch), Toast.LENGTH_LONG).show();
//						break;
					case TracksCommunicator.UPDATE_FAIL_CODE:
						p.dismiss();
						Toast.makeText(context, getString(R.string.ERR_upload), Toast.LENGTH_LONG).show();
						break;
					case TracksCommunicator.SUCCESS_CODE:						
						p.dismiss();
						
						updateListView();
												
						break;
					}
				}
				
			});
		
		Message.obtain(_commHandler, 0, a).sendToTarget();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo)menuInfo;
		if(info.targetView instanceof TaskListItem) {
			MenuInflater inflater = getMenuInflater();
			TaskListItem tli = (TaskListItem)info.targetView;
			inflater.inflate(R.menu.task_context_menu, menu);
			menu.setHeaderTitle(tli.getTask().getDescription());
		} else {
			int gid = getExpandableListView().getPackedPositionGroup(info.packedPosition);
			TodoContext c = (TodoContext)_tla.getGroup(gid);
			Intent i = new Intent(this, ContextEditorActivity.class);
			i.putExtra("context", c.getDbKeyId());
			startActivityForResult(i, EDIT_CONTEXT);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo)item.getMenuInfo();
		int cid = getExpandableListView().getPackedPositionChild(info.packedPosition);
		int gid = getExpandableListView().getPackedPositionGroup(info.packedPosition);
		Task t = (Task)_tla.getChild(gid, cid);
		String desc = t.getDescription();
		Context context = getExpandableListView().getContext();
		TracksAction a;
		
		switch(item.getItemId()) {
		case R.id.edit_task:
			Intent i = new Intent(this, TaskEditorActivity.class);
			i.putExtra("task", t.getDbKeyId());
			startActivityForResult(i, EDIT_TASK);
			return true;
		case R.id.delete_task:
			a = new TracksAction(TracksAction.ActionType.DELETE_TASK, t,
								 _tla.getNotifyHandler());
			Message.obtain(_commHandler, 0, a).sendToTarget();			
			return true;
		case R.id.done_task:
			t.setDone(true);
			a = new TracksAction(TracksAction.ActionType.COMPLETE_TASK,
								 t, _tla.getNotifyHandler());
			_commHandler.obtainMessage(0, a).sendToTarget();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == SETTINGS) {
			Log.v(TAG, "Returned from settings");
			configurePersistence();
			refreshList();
		}
		
		if(requestCode == NEW_TASK || requestCode == EDIT_TASK) {
			Log.v(TAG, "Returned from edit");
			if(resultCode == TaskEditorActivity.SAVED) {
				Log.v(TAG, "Task was saved");
				_tla.notifyDataSetChanged();
			}
		}

		if(requestCode == NEW_CONTEXT || requestCode == EDIT_CONTEXT) {
			Log.v(TAG, "Returned from edit");
			if(resultCode == ContextEditorActivity.SAVED) {
				Log.v(TAG, "Context was saved");
				_tla.notifyDataSetChanged();
			}
		}

		if(requestCode == NEW_PROJECT || requestCode == EDIT_PROJECT) {
			Log.v(TAG, "Returned from edit");
			if(resultCode == ProjectEditorActivity.SAVED) {
				Log.v(TAG, "Project was saved");
				_tla.notifyDataSetChanged();
			}
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView l, View view, int group, int position, long id) {
		Task t = (Task)_tla.getChild(group, position);

		Intent i = new Intent(this, TaskEditorActivity.class);
		i.putExtra("task", t.getDbKeyId());
		startActivityForResult(i, EDIT_TASK);
		
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inf = getMenuInflater();
		inf.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.MENU_add:
			startActivityForResult(new Intent(this, TaskEditorActivity.class), NEW_TASK);
			return true;
		case R.id.MENU_addcontext:
			startActivityForResult(new Intent(this, ContextEditorActivity.class), NEW_CONTEXT);
			return true;
		case R.id.MENU_addproject:
			startActivityForResult(new Intent(this, ProjectEditorActivity.class), NEW_PROJECT);
			return true;
		case R.id.MENU_settings:
			startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS);
			return true;
		case R.id.MENU_refresh:
			refreshList();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.i(TAG, "Configuration changed.");
		getExpandableListView().requestLayout();
	}

	private void updateListView() {
		_tla.notifyDataSetChanged();
		expandAllGroups();
	}
	
	private void expandAllGroups() {
		int ngroups = _tla.getGroupCount();
		for(int g = 0; g < ngroups; g++) {
			if(!((TodoContext)_tla.getGroup(g)).isHidden()) {
				getExpandableListView().expandGroup(g);
			}
		}
	}
}
