package ca.xvx.tracks;

import android.util.Log;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

public class TaskEditorActivity extends Activity {
	private static final String TAG = "TaskEditorActivity";

	private static final int NEW_CONTEXT = 1;
	private static final int NEW_PROJECT = 2;
	
	private EditText _description;
	private EditText _notes;
	private Spinner _project;
	private Spinner _context;
	private Date _due;
	private Date _showfrom;
	private Button _dueButt;
	private Button _showButt;

	private Task _task;
	private Handler _commHandler;

	public static final int SAVED = 0;
	public static final int CANCELED = 1;

	private static final int SHOW_FROM = 0;
	private static final int DUE = 1;
	
	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setContentView(R.layout.taskeditor_activity);

		Button saveButt;
		Button cancelButt;
		Button newCButt, newPButt;

		ArrayAdapter<TodoContext> cad;
		ArrayAdapter<Project> pad;

		final java.text.DateFormat dform = DateFormat.getDateFormat(this);

		Intent intent = getIntent();
		_description = (EditText)findViewById(R.id.TEA_description);
		_notes = (EditText)findViewById(R.id.TEA_notes);
		_project = (Spinner)findViewById(R.id.TEA_project);
		_context = (Spinner)findViewById(R.id.TEA_context);

		_dueButt = (Button)findViewById(R.id.TEA_due_date);
		_showButt = (Button)findViewById(R.id.TEA_show_from);

		newCButt = (Button)findViewById(R.id.TEA_add_context);
		newPButt = (Button)findViewById(R.id.TEA_add_project);
		
		saveButt = (Button)findViewById(R.id.TEA_save);
		cancelButt = (Button)findViewById(R.id.TEA_cancel);
		
		cad = refreshContexts();
		pad = refreshProjects();

		_commHandler = TracksCommunicator.getHandler();
		long tno = intent.getLongExtra("task", -1);
		if(tno >= 0) {
			_task = Task.getTask(tno);
			
			_description.setText(_task.getDescription());
			_notes.setText(_task.getNotes());
			_due = _task.getDue();
			_showfrom = _task.getShowFrom();
			if(_due != null) {
				_dueButt.setText(dform.format(_due));
			}
			if(_showfrom != null) {
				_showButt.setText(dform.format(_showfrom));
			}

			TodoContext c = _task.getContext();
			Project p = _task.getProject();
			_context.setSelection(cad.getPosition(c));
			if(p != null) {
				_project.setSelection(pad.getPosition(p));
			}
		}

		newCButt.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.v(TAG, "Going to new context");
					startActivityForResult(new Intent(v.getContext(), ContextEditorActivity.class), NEW_CONTEXT);
				}
			});

		newPButt.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.v(TAG, "Going to new context");
					startActivityForResult(new Intent(v.getContext(), ProjectEditorActivity.class), NEW_PROJECT);
				}
			});

		saveButt.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.v(TAG, "Edit saved");
					save();
				}
			});

		cancelButt.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.v(TAG, "Edit cancelled");
					setResult(CANCELED);
					finish();
				}
			});

		_dueButt.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.v(TAG, "Due pressed");
					showDialog(DUE);
				}
			});
		_dueButt.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					Log.v(TAG, "Due cleared");
					_dueButt.setText("");
					_due = null;
					return true;
				}
			});
		
		_showButt.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.v(TAG, "Show from pressed");
					showDialog(SHOW_FROM);
				}
			});
		_showButt.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					Log.v(TAG, "Show from cleared");
					_showButt.setText("");
					_showfrom = null;
					return true;
				}
			});
	}

	private ArrayAdapter<Project> refreshProjects() {
		ArrayAdapter<Project> pad = new ArrayAdapter<Project>(this, android.R.layout.simple_spinner_item,
															  Project.getActiveProjects().toArray(new Project[0]));
		pad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		pad.sort(new Comparator<Project>() {
				@Override
				public int compare(Project a, Project b) {
					return a.getPosition() - b.getPosition();
				}
			});
		_project.setAdapter(pad);

		return pad;
	}
	
	private ArrayAdapter<TodoContext> refreshContexts() {
		ArrayAdapter<TodoContext> cad = new ArrayAdapter<TodoContext>(this, android.R.layout.simple_spinner_item,
																	  TodoContext.getAllContexts().toArray(new TodoContext[0]));
		cad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		cad.sort(new Comparator<TodoContext>() {
				@Override
				public int compare(TodoContext a, TodoContext b) {
					return a.getPosition() - b.getPosition();
				}
			});
		_context.setAdapter(cad);
		
		return cad;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case NEW_CONTEXT:
			refreshContexts();
			break;
		case NEW_PROJECT:
			refreshProjects();
			break;
		default:
		}
	}

	private void save() {
		final String oldDesc, oldNotes;
		final TodoContext oldContext;
		final Project oldProject;
		final Date oldDue, oldShowFrom;
		final Context context = this;

		Log.d(TAG, "Saving task!");
		
		Project newProject = (Project)_project.getSelectedItem();
		if(newProject != null && newProject.getId() < 0) {
			newProject = null;
		}
		TodoContext newContext = (TodoContext)_context.getSelectedItem();
		
		// Must have a description
		if(_description.length() <= 0) {
			Log.w(TAG, "Attempted to save with no description");
			Toast.makeText(context, R.string.ERR_save_baddata, Toast.LENGTH_LONG).show();
			return;
		}

		if(_task == null) {
			Log.d(TAG, "Creating a new task");
			_task = new Task(_description.getText().toString(), _notes.getText().toString(), newContext,
							 newProject, _due, _showfrom);
			oldDesc = _task.getDescription();
			oldNotes = _task.getNotes();
			oldContext = _task.getContext();
			oldProject = _task.getProject();
			oldDue = _task.getDue();
			oldShowFrom = _task.getShowFrom();
		} else {
			Log.d(TAG, "Updating an existing task");
			oldDesc = _task.setDescription(_description.getText().toString());
			oldNotes = _task.setNotes(_notes.getText().toString());
			oldContext = _task.setContext(newContext);
			oldProject = _task.setProject(newProject);
			oldDue = _task.setDue(_due);
			oldShowFrom = _task.setShowFrom(_showfrom);
		}

		final ProgressDialog p = ProgressDialog.show(context, "", getString(R.string.MSG_saving), true);
		TracksAction a = new TracksAction(TracksAction.ActionType.UPDATE_TASK, _task, new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch(msg.what) {
					case TracksCommunicator.SUCCESS_CODE:
						Log.d(TAG, "Saved successfully");
						_task.setOutdated(false);
						Task.save(_task);
						p.dismiss();
						setResult(SAVED);
						finish();
						break;
					case TracksCommunicator.UPDATE_FAIL_CODE:
						Log.w(TAG, "Save failed");
						_task.setOutdated(true);
						Task.save(_task);
						p.dismiss();
						setResult(SAVED);		
						finish();
//						Toast.makeText(context, R.string.ERR_save_general, Toast.LENGTH_LONG).show();
						// Reset task data to stay synced with server.
//						_task.setDescription(oldDesc);
//						_task.setNotes(oldNotes);
//						_task.setContext(oldContext);
//						_task.setProject(oldProject);
//						_task.setDue(oldDue);
//						_task.setShowFrom(oldShowFrom);
						break;
					}
				}
			});
		_commHandler.obtainMessage(0, a).sendToTarget();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Calendar initial = Calendar.getInstance();
		Date st;
		final int did = id;
		
		switch(id) {
		case SHOW_FROM:
			st = _showfrom == null ? null : _showfrom;
			break;
		case DUE:
			st = _due == null ? null : _due;
			break;
		default:
			return null;
		}

		if(st != null) {
			initial.setTime(st);
		}
			
		final java.text.DateFormat dform = DateFormat.getDateFormat(this);
		return new DatePickerDialog(this,
									new DatePickerDialog.OnDateSetListener() {
										@Override
										public void onDateSet(DatePicker v, int year, int month, int day) {
											Calendar c = Calendar.getInstance();
											c.set(year, month, day);
											if(did == SHOW_FROM) {
												_showfrom = c.getTime();
												_showButt.setText(dform.format(_showfrom));
											} else if(did == DUE) {
												_due = c.getTime();
												_dueButt.setText(dform.format(_due));
											}
										}
									},
									initial.get(Calendar.YEAR),
									initial.get(Calendar.MONTH),
									initial.get(Calendar.DAY_OF_MONTH));
	}
}
