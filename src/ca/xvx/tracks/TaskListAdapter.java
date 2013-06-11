package ca.xvx.tracks;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

public class TaskListAdapter extends BaseExpandableListAdapter {
	private Vector<TodoContext> _contexts;
	private SortedMap<TodoContext, Vector<Task>> _tasks;

	private Handler _notifyHandler;

	public TaskListAdapter() {
		super();

		_contexts = new Vector<TodoContext>();
		_tasks = new TreeMap<TodoContext, Vector<Task>>();
	}

	public Handler getNotifyHandler() {
		if(_notifyHandler == null) {
			_notifyHandler = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						notifyDataSetChanged();
					}
				};
		}

		return _notifyHandler;
	}

	@Override
	public void notifyDataSetChanged() {
		_contexts.clear();
		_tasks.clear();
		Task.loadAllTasks();
		for(Task t : Task.getAllTasks()) {
			if(!t.getContext().isHidden() && !t.isDeleted() && !t.getDone()) {
				if(_tasks.get(t.getContext()) == null) {
					_contexts.add(t.getContext());
					_tasks.put(t.getContext(), new Vector<Task>());
				}
				_tasks.get(t.getContext()).add(t);
			}
			
		}

		Collections.sort(_contexts);
		for(TodoContext c : _contexts) {
			if(!c.isHidden()) {
				Collections.sort(_tasks.get(c));
			}
		}

		super.notifyDataSetChanged();
	}

	@Override
	public View getChildView(int group, int position, boolean isLastChild, View convert, ViewGroup parent) {
		TodoContext con = _contexts.get(group);
		if(con == null) {
			return null;
		}
		
		Task t = _tasks.get(con).get(position);
		if(t == null) {
			return null;
		}

		return new TaskListItem(parent.getContext(), t, getNotifyHandler());
	}

	@Override
	public View getGroupView(int group, boolean isExpanded, View convert, ViewGroup parent) {
		TodoContext con = _contexts.get(group);
		if(con == null) {
			return null;
		}

		LayoutInflater inflater = (LayoutInflater)parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View ret = inflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
		TextView t = (TextView)ret.findViewById(android.R.id.text1);
		t.setText(con.getName());

		return ret;
	}

	@Override
	public int getChildrenCount(int group) {
		TodoContext con = _contexts.get(group);
		if(con == null) {
			return 0;
		}
		return _tasks.get(con).size();
	}

	@Override
	public int getGroupCount() {
		return _contexts.size();
	}

	@Override
	public Object getChild(int group, int pos) {
		TodoContext con = _contexts.get(group);
		return _tasks.get(con).get(pos);
	}

	@Override
	public long getChildId(int group, int pos) {
		return pos;
	}

	@Override
	public long getGroupId(int group) {
		return group;
	}

	@Override
	public Object getGroup(int group) {
		return _contexts.get(group);
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int g, int p) {
		return true;
	}
}
